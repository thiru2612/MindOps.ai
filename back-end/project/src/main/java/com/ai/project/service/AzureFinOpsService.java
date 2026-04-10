package com.ai.project.service;

import com.ai.project.dto.AzureResourceDto;
import com.ai.project.dto.FinOpsDashboardResponse;
import com.ai.project.entity.User;
import com.ai.project.service.CredentialVaultService.DecryptedAzureCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Azure FinOps service providing a live, read-only cost dashboard for all
 * Azure resources tagged with {@code ManagedBy=MindOps} under a given credential.
 *
 * <p><strong>Architecture overview:</strong>
 * <ol>
 *   <li>Decrypts the user's Azure Service Principal credentials via
 *       {@link CredentialVaultService}.</li>
 *   <li>Authenticates a {@link ResourceManager} client using
 *       {@link com.azure.identity.ClientSecretCredential}.</li>
 *   <li>Queries the Azure Resource Manager API for all resources tagged
 *       {@code ManagedBy=MindOps} within the subscription.</li>
 *   <li>For each Compute VM resource, extracts the hardware profile SKU
 *       (e.g. {@code Standard_B1s}) for pricing lookup.</li>
 *   <li>Calls the public, unauthenticated
 *       <a href="https://prices.azure.com/api/retail/prices">Azure Retail Prices API</a>
 *       per resource to compute {@code retailPrice × 730} monthly estimates.</li>
 *   <li>Aggregates per-resource costs into a {@code totalMonthlyCost} and returns
 *       a complete {@link FinOpsDashboardResponse} snapshot.</li>
 * </ol>
 * </p>
 *
 * <p><strong>Resilience contract:</strong> Pricing API failures for individual
 * resources are caught and logged as warnings. The dashboard still returns all
 * discovered resources — affected entries show {@code null} for
 * {@code estimatedMonthlyCost} rather than failing the entire request. This
 * ensures partial data is always more useful than a hard 500 error.</p>
 *
 * <p><strong>Cost calculation note:</strong> Prices reflect Azure public retail
 * rates (pay-as-you-go). Reserved instance pricing, Enterprise Agreement
 * discounts, hybrid benefits, and egress costs are not accounted for.</p>
 */
@Slf4j
@Service
public class AzureFinOpsService {

    private static final String  MANAGED_BY_TAG_KEY       = "ManagedBy";
    private static final String  MANAGED_BY_TAG_VALUE     = "MindOps";
    private static final String  AZURE_RETAIL_PRICES_URL  =
        "https://prices.azure.com/api/retail/prices";
    private static final double  HOURS_PER_MONTH          = 730.0;
    private static final String  VM_RESOURCE_TYPE         =
        "Microsoft.Compute/virtualMachines";
    private static final String  PRICING_DISCLAIMER       =
        "Cost estimates are based on Azure public retail (pay-as-you-go) prices and may differ " +
        "from actual billed amounts. Reserved instance discounts, hybrid benefits, and data " +
        "transfer costs are not reflected.";

    private final CredentialVaultService credentialVaultService;
    private final UserService            userService;
    private final RestClient             pricingRestClient;
    private final ObjectMapper           objectMapper;

    public AzureFinOpsService(
        CredentialVaultService credentialVaultService,
        UserService            userService,
        ObjectMapper           objectMapper
    ) {
        this.credentialVaultService = credentialVaultService;
        this.userService            = userService;
        this.objectMapper           = objectMapper;

        // Dedicated RestClient for the Azure Retail Prices API (public, no auth)
        this.pricingRestClient = RestClient.builder()
            .baseUrl(AZURE_RETAIL_PRICES_URL)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds a live FinOps dashboard snapshot for all MindOps-tagged Azure
     * resources accessible via the specified credential.
     *
     * @param credentialId the {@code public_id} of a stored Azure credential
     *                     belonging to the authenticated user
     * @return a fully-populated {@link FinOpsDashboardResponse} with per-resource
     *         cost estimates and a total monthly cost aggregation
     * @throws com.ai.project.exception.ResourceNotFoundException if the credential
     *         does not exist or is not owned by the authenticated user
     * @throws IllegalStateException if Azure authentication fails or the
     *         Resource Manager API is unreachable
     */
    public FinOpsDashboardResponse getLiveDashboard(String credentialId) {
        User currentUser = userService.resolveCurrentUser();

        log.info("[AzureFinOpsService] Building FinOps dashboard for credential '{}', user '{}'.",
            credentialId, currentUser.getEmail());

        // ── 1. Decrypt credentials ────────────────────────────────────────────
        DecryptedAzureCredential credentials = credentialVaultService
            .decryptAzureCredential(credentialId, currentUser);

        // ── 2. Authenticate and build ResourceManager client ──────────────────
        ResourceManager resourceManager = buildResourceManager(credentials);

        // ── 3. Query all MindOps-tagged resources in the subscription ─────────
        List<GenericResource> taggedResources = fetchTaggedResources(resourceManager);

        log.info("[AzureFinOpsService] Found {} MindOps-tagged resource(s) in subscription.",
            taggedResources.size());

        // ── 4. Map each resource to a DTO with pricing enrichment ─────────────
        List<AzureResourceDto> resourceDtos = new ArrayList<>();
        AtomicReference<Double> totalCost   = new AtomicReference<>(0.0);

        for (GenericResource resource : taggedResources) {
            AzureResourceDto dto = buildResourceDto(resource);
            resourceDtos.add(dto);

            if (dto.getEstimatedMonthlyCost() != null) {
                totalCost.updateAndGet(current -> current + dto.getEstimatedMonthlyCost());
            }
        }

        // ── 5. Build and return the dashboard response ────────────────────────
        FinOpsDashboardResponse dashboard = FinOpsDashboardResponse.builder()
            .provider("AZURE")
            .credentialId(credentialId)
            .totalMonthlyCost(Math.round(totalCost.get() * 100.0) / 100.0)
            .resourceCount(resourceDtos.size())
            .pricingCurrency("USD")
            .resources(resourceDtos)
            .generatedAt(LocalDateTime.now())
            .pricingDisclaimer(PRICING_DISCLAIMER)
            .build();

        log.info("[AzureFinOpsService] Dashboard built. Resources: {}, Total estimated cost: ${}/month.",
            resourceDtos.size(), dashboard.getTotalMonthlyCost());

        return dashboard;
    }

    // ── Internal: Resource Discovery ─────────────────────────────────────────

    /**
     * Fetches all resources in the subscription tagged with
     * {@code ManagedBy=MindOps} using the Azure Resource Manager's generic
     * resources list API, filtered server-side by tag key/value.
     *
     * @param resourceManager the authenticated ResourceManager client
     * @return list of matching {@link GenericResource} instances (may be empty)
     * @throws IllegalStateException if the ARM API call fails
     */
    // private List<GenericResource> fetchTaggedResources(ResourceManager resourceManager) {
    //     try {
    //         List<GenericResource> results = new ArrayList<>();

    //         resourceManager.genericResources()
    //             .listByTag(MANAGED_BY_TAG_KEY, MANAGED_BY_TAG_VALUE)
    //             .forEach(results::add);

    //         return results;

    //     } catch (com.azure.core.management.exception.ManagementException e) {
    //         log.error("[AzureFinOpsService] ARM API error fetching tagged resources. " +
    //                   "Code: {}, Message: {}",
    //             e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
    //             e.getMessage());
    //         throw new IllegalStateException(
    //             "Failed to query Azure resources: " + e.getMessage()
    //         );
    //     } catch (Exception e) {
    //         log.error("[AzureFinOpsService] Unexpected error fetching tagged resources: {}",
    //             e.getMessage());
    //         throw new IllegalStateException(
    //             "Failed to query Azure resources: " + e.getMessage()
    //         );
    //     }
    // }
    private List<GenericResource> fetchTaggedResources(ResourceManager resourceManager) {
        try {
            List<GenericResource> results = new ArrayList<>();

            // Fetch all resources and filter by tag in-memory to bypass SDK signature mismatches
            resourceManager.genericResources().list().stream()
                .filter(res -> res.tags() != null && 
                               MANAGED_BY_TAG_VALUE.equals(res.tags().get(MANAGED_BY_TAG_KEY)))
                .forEach(results::add);

            return results;

        } catch (com.azure.core.management.exception.ManagementException e) {
            log.error("[AzureFinOpsService] ARM API error fetching tagged resources. " +
                      "Code: {}, Message: {}",
                e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
                e.getMessage());
            throw new IllegalStateException(
                "Failed to query Azure resources: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("[AzureFinOpsService] Unexpected error fetching tagged resources: {}",
                e.getMessage());
            throw new IllegalStateException(
                "Failed to query Azure resources: " + e.getMessage()
            );
        }
    }

    // ── Internal: Resource → DTO Mapping ─────────────────────────────────────

    /**
     * Maps a {@link GenericResource} to an {@link AzureResourceDto}, extracting
     * the SKU and fetching a pricing estimate for the resource.
     *
     * <p>SKU extraction strategy:
     * <ul>
     *   <li>For {@code Microsoft.Compute/virtualMachines}: The hardware profile
     *       size is stored on the full VM object via the ARM API. For generic
     *       resource list results, the SKU may be available directly on the
     *       resource SKU field — we check both paths.</li>
     *   <li>For non-VM resources: The resource's {@code sku().name()} is used
     *       if present; otherwise {@code null} is set and no pricing lookup
     *       is attempted.</li>
     * </ul>
     * </p>
     *
     * @param resource the raw ARM generic resource
     * @return an enriched {@link AzureResourceDto}
     */
    private AzureResourceDto buildResourceDto(GenericResource resource) {
        String resourceId = resource.id();
        String name       = resource.name();
        String type       = resource.type();
        String location   = resource.regionName();
        String sku        = extractSku(resource);

        log.debug("[AzureFinOpsService] Processing resource: name={}, type={}, location={}, sku={}",
            name, type, location, sku);

        Double monthlyCost = null;

        if (sku != null && !sku.isBlank() && location != null && !location.isBlank()) {
            monthlyCost = fetchSkuMonthlyCost(sku, location);
            if (monthlyCost != null) {
                // Round to 2 decimal places for clean display
                monthlyCost = Math.round(monthlyCost * 100.0) / 100.0;
            }
        } else {
            log.debug("[AzureFinOpsService] Skipping pricing lookup for '{}' — " +
                      "no SKU or location available.", name);
        }

        return AzureResourceDto.builder()
            .resourceId(resourceId)
            .name(name)
            .type(type)
            .location(location)
            .sku(sku)
            .estimatedMonthlyCost(monthlyCost)
            .build();
    }

    /**
     * Extracts the most specific SKU name available from a generic resource.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Resource's own {@code sku().name()} (available for Storage Accounts,
     *       App Service Plans, SQL Databases, etc.)</li>
     *   <li>For VM type resources, fall back to checking the resource's
     *       {@code properties} map for {@code hardwareProfile.vmSize}
     *       (available when the generic resource is fetched with expanded properties).</li>
     *   <li>{@code null} if neither is present.</li>
     * </ol>
     * </p>
     */
    private String extractSku(GenericResource resource) {
        // Path 1: Direct SKU on the generic resource (works for Storage, SQL, App Service)
        if (resource.sku() != null && resource.sku().name() != null
                && !resource.sku().name().isBlank()) {
            return resource.sku().name();
        }

        // Path 2: VM hardware profile embedded in resource properties JSON
        if (resource.type() != null &&
                resource.type().equalsIgnoreCase(VM_RESOURCE_TYPE)) {
            try {
                Object properties = resource.properties();
                if (properties != null) {
                    String propsJson  = objectMapper.writeValueAsString(properties);
                    JsonNode propsNode = objectMapper.readTree(propsJson);
                    JsonNode vmSize   = propsNode
                        .path("hardwareProfile")
                        .path("vmSize");
                    if (!vmSize.isMissingNode() && !vmSize.isNull()) {
                        return vmSize.asText();
                    }
                }
            } catch (Exception e) {
                log.debug("[AzureFinOpsService] Could not extract VM size from properties " +
                          "for resource '{}': {}", resource.name(), e.getMessage());
            }
        }

        return null;
    }

    // ── Internal: Azure Retail Prices API ────────────────────────────────────

    /**
     * Queries the public Azure Retail Prices API to fetch the hourly consumption
     * price for a given SKU in a specified region, then converts it to a monthly
     * estimate by multiplying by 730 (average hours per month).
     *
     * <p><strong>API:</strong> {@code https://prices.azure.com/api/retail/prices}
     * No authentication is required — this is a fully public Microsoft endpoint.</p>
     *
     * <p><strong>Filter construction:</strong>
     * <pre>
     * armRegionName eq 'eastus' and armSkuName eq 'Standard_B1s' and priceType eq 'Consumption'
     * </pre>
     * The filter is URL-encoded before being appended as the {@code $filter} query parameter.</p>
     *
     * <p><strong>Response parsing:</strong> The API returns a {@code Items} array.
     * We take the first matching item's {@code retailPrice} field, which represents
     * the hourly USD pay-as-you-go rate with no reservations applied.</p>
     *
     * <p><strong>Resilience:</strong> Any exception (network timeout, parse failure,
     * empty response, unexpected JSON shape) is caught and logged as a warning.
     * The method returns {@code null} in all failure cases — the caller handles
     * {@code null} by omitting the cost field from the DTO rather than crashing.</p>
     *
     * @param skuName the ARM SKU name (e.g. {@code Standard_B1s}, {@code Standard_LRS})
     * @param region  the Azure ARM region name (e.g. {@code eastus}, {@code westeurope})
     * @return the estimated monthly cost in USD, or {@code null} if unavailable
     */
    private Double fetchSkuMonthlyCost(String skuName, String region) {
        String normalizedRegion = normalizeRegion(region);

        String filterExpression = String.format(
            "armRegionName eq '%s' and armSkuName eq '%s' and priceType eq 'Consumption'",
            normalizedRegion, skuName
        );

        String encodedFilter;
        try {
            encodedFilter = URLEncoder.encode(filterExpression, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[AzureFinOpsService] Failed to URL-encode pricing filter for SKU '{}': {}",
                skuName, e.getMessage());
            return null;
        }

        log.debug("[AzureFinOpsService] Fetching pricing for SKU '{}' in region '{}'.",
            skuName, normalizedRegion);

        try {
            String responseBody = pricingRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("$filter", filterExpression)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.warn("[AzureFinOpsService] Azure Retail Prices API returned HTTP {} " +
                             "for SKU '{}' in region '{}'.",
                        res.getStatusCode().value(), skuName, normalizedRegion);
                    throw new IllegalStateException(
                        "Pricing API error: HTTP " + res.getStatusCode().value()
                    );
                })
                .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("[AzureFinOpsService] Empty response from Pricing API for SKU '{}'. " +
                         "Returning null cost.", skuName);
                return null;
            }

            return parseRetailPriceFromResponse(responseBody, skuName, normalizedRegion);

        } catch (IllegalStateException e) {
            // HTTP error status — already logged above
            return null;
        } catch (Exception e) {
            log.warn("[AzureFinOpsService] Pricing API call failed for SKU '{}' in '{}': {}. " +
                     "Returning null cost — dashboard will continue without this estimate.",
                skuName, normalizedRegion, e.getMessage());
            return null;
        }
    }

    /**
     * Parses the {@code retailPrice} from the Azure Retail Prices API response body
     * and converts the hourly rate to a monthly estimate.
     *
     * <p>Expected response shape:
     * <pre>
     * {
     *   "BillingCurrency": "USD",
     *   "CustomerEntityId": "Default",
     *   "Items": [
     *     {
     *       "currencyCode": "USD",
     *       "retailPrice": 0.0104,
     *       "unitPrice": 0.0104,
     *       "armRegionName": "eastus",
     *       "armSkuName": "Standard_B1s",
     *       "productName": "Virtual Machines BS Series",
     *       "skuName": "B1s",
     *       "serviceName": "Virtual Machines",
     *       "priceType": "Consumption",
     *       ...
     *     }
     *   ]
     * }
     * </pre>
     * </p>
     *
     * @return hourly {@code retailPrice} × 730, or {@code null} if no items returned
     */
    private Double parseRetailPriceFromResponse(
        String responseBody,
        String skuName,
        String region
    ) {
        try {
            JsonNode root      = objectMapper.readTree(responseBody);
            JsonNode itemsNode = root.get("Items");

            if (itemsNode == null || !itemsNode.isArray() || itemsNode.isEmpty()) {
                log.info("[AzureFinOpsService] No pricing items returned for SKU '{}' " +
                         "in region '{}'. SKU may not exist in this region or under this " +
                         "priceType filter.", skuName, region);
                return null;
            }

            // Take the first matching item — the filter is precise enough that the
            // first result is the correct Consumption-type hourly rate
            JsonNode firstItem     = itemsNode.get(0);
            JsonNode retailPriceNode = firstItem.get("retailPrice");

            if (retailPriceNode == null || retailPriceNode.isNull()) {
                log.warn("[AzureFinOpsService] 'retailPrice' field absent in pricing response " +
                         "for SKU '{}'. Response item: {}", skuName, firstItem);
                return null;
            }

            double hourlyRate = retailPriceNode.asDouble();

            if (hourlyRate <= 0.0) {
                log.debug("[AzureFinOpsService] Retail price for SKU '{}' is {} — " +
                          "may be a free-tier resource.", skuName, hourlyRate);
                return 0.0;
            }

            double monthlyCost = hourlyRate * HOURS_PER_MONTH;

            log.debug("[AzureFinOpsService] SKU '{}' in '{}': hourly=${}, monthly=${}",
                skuName, region, hourlyRate, String.format("%.2f", monthlyCost));

            return monthlyCost;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("[AzureFinOpsService] Failed to parse Pricing API JSON response " +
                     "for SKU '{}': {}", skuName, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("[AzureFinOpsService] Unexpected error parsing pricing response " +
                     "for SKU '{}': {}", skuName, e.getMessage());
            return null;
        }
    }

    // ── Internal: Azure client construction ──────────────────────────────────

    /**
     * Constructs a fully-authenticated {@link ResourceManager} client using
     * the decrypted Service Principal credentials.
     *
     * @throws IllegalStateException if Azure authentication fails
     */
    private ResourceManager buildResourceManager(DecryptedAzureCredential credentials) {
        try {
            com.azure.core.credential.TokenCredential tokenCredential =
                new ClientSecretCredentialBuilder()
                    .tenantId(credentials.tenantId())
                    .clientId(credentials.clientId())
                    .clientSecret(credentials.clientSecret())
                    .build();

            AzureProfile azureProfile = new AzureProfile(
                credentials.tenantId(),
                credentials.subscriptionId(),
                AzureEnvironment.AZURE
            );

            return ResourceManager
                .authenticate(tokenCredential, azureProfile)
                .withSubscription(credentials.subscriptionId());

        } catch (Exception e) {
            log.error("[AzureFinOpsService] Failed to build Azure ResourceManager client: {}",
                e.getMessage());
            throw new IllegalStateException(
                "Azure authentication failed. Please verify your credential is valid: "
                + e.getMessage()
            );
        }
    }

    /**
     * Normalises an Azure region name to the ARM lowercase format expected by
     * the Retail Prices API filter.
     *
     * <p>Examples:
     * <pre>
     *   "East US"      → "eastus"
     *   "West Europe"  → "westeurope"
     *   "eastus"       → "eastus"   (already normalised)
     * </pre>
     * The ARM region names used by ResourceManager are already lowercase
     * without spaces — this method handles edge cases where a display-format
     * region name is passed in.</p>
     */
    private String normalizeRegion(String region) {
        if (region == null) return "eastus";
        return region.toLowerCase().replaceAll("\\s+", "");
    }
}