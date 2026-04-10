// package com.ai.project.service;

// import com.ai.project.service.CredentialVaultService.DecryptedAzureCredential;
// import com.azure.core.credential.TokenCredential;
// import com.azure.core.management.AzureEnvironment;
// import com.azure.core.management.Region;
// import com.azure.core.management.profile.AzureProfile;
// import com.azure.identity.ClientSecretCredentialBuilder;
// import com.azure.resourcemanager.compute.ComputeManager;
// import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
// import com.azure.resourcemanager.compute.models.VirtualMachine;
// import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
// import com.azure.resourcemanager.resources.ResourceManager;
// import com.fasterxml.jackson.databind.JsonNode;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// /**
//  * Azure provisioning service — scoped to Linux VMs for the Phase 5 MVP.
//  *
//  * <p><strong>Authentication:</strong> Uses a Service Principal
//  * ({@code clientId} + {@code clientSecret} + {@code tenantId}) via
//  * {@link com.azure.identity.ClientSecretCredential}. This is the standard
//  * non-interactive auth flow for server-side Azure automation.</p>
//  *
//  * <p><strong>Resource Group strategy:</strong> Each deployment creates a
//  * dedicated Resource Group named {@code mindops-rg-<timestamp>}. This provides
//  * clean teardown isolation — deleting the Resource Group cascades to all
//  * contained resources (VM, NIC, disk, public IP, VNet) atomically, avoiding
//  * the complex reverse-dependency deletion order required for individual resources.</p>
//  *
//  * <p><strong>VM image:</strong> Uses {@link KnownLinuxVirtualMachineImage#UBUNTU_SERVER_18_04_LTS}
//  * — a well-known, stable public image available in all Azure regions. Phase 6
//  * can expose image selection via the Gemini config JSON.</p>
//  *
//  * <p><strong>MVP scope:</strong> Provisions a single Linux VM. Phase 6 will
//  * expand to App Service, AKS, Azure SQL, and Load Balancer based on the
//  * full {@code sdkParamsJson}.</p>
//  */
// @Slf4j
// @Service
// public class AzureProvisioningService {

//     private static final String MANAGED_BY_TAG     = "ManagedBy";
//     private static final String MANAGED_BY_VALUE   = "MindOps";
//     private static final String APP_TAG            = "Application";
//     private static final String APP_VALUE          = "MindOpsCloud";
//     private static final String VM_ADMIN_USER      = "mindopsadmin";

//     /**
//      * A strong default admin password meeting Azure complexity requirements.
//      * In Phase 6 this should be replaced with SSH public key authentication —
//      * passwords are acceptable only for MVP bootstrap.
//      */
//     private static final String VM_ADMIN_PASSWORD  = "M!ndOps2025#Secure";

//     // ── Provision Azure VM ────────────────────────────────────────────────────

//     /**
//      * Provisions a Linux VM in a new dedicated Resource Group.
//      *
//      * <p>Provisioning sequence:
//      * <ol>
//      *   <li>Build {@link TokenCredential} from the Service Principal credentials.</li>
//      *   <li>Create a Resource Group in the specified region.</li>
//      *   <li>Define and create the VM (Azure SDK handles NIC, public IP, OS disk
//      *       implicitly when using the fluent builder's {@code withNewPrimaryNetwork}
//      *       and {@code withNewPrimaryPublicIPAddress} methods).</li>
//      *   <li>Return the VM's fully-qualified Azure Resource ID (used for teardown).</li>
//      * </ol>
//      * </p>
//      *
//      * @param config      the validated Gemini JSON config node
//      * @param credentials decrypted Azure Service Principal credentials
//      * @return the Azure VM resource ID (e.g. {@code /subscriptions/.../virtualMachines/mindops-vm-...})
//      * @throws IllegalStateException if the Azure SDK throws any management exception
//      */
//     public String provisionVm(JsonNode config, DecryptedAzureCredential credentials) {
//         String region    = resolveRegion(config);
//         String vmSize    = resolveVmSize(config);
//         String rgName    = "mindops-rg-" + System.currentTimeMillis();
//         String vmName    = "mindops-vm-" + System.currentTimeMillis();

//         log.info("[AzureProvisioningService] Provisioning Azure VM: name={}, size={}, region={}, rg={}",
//             vmName, vmSize, region, rgName);

//         try {
//             TokenCredential tokenCredential = buildTokenCredential(credentials);
//             AzureProfile    azureProfile    = buildAzureProfile(credentials);

//             // Build ComputeManager — the entry point for all compute operations
//             ComputeManager computeManager = ComputeManager
//                 .authenticate(tokenCredential, azureProfile);

//             // Build ResourceManager — needed for Resource Group creation
//             ResourceManager resourceManager = ResourceManager
//                 .authenticate(tokenCredential, azureProfile)
//                 .withSubscription(credentials.subscriptionId());

//             // Step 1: Create dedicated Resource Group for this deployment
//             resourceManager.resourceGroups()
//                 .define(rgName)
//                 .withRegion(region)
//                 .withTag(MANAGED_BY_TAG, MANAGED_BY_VALUE)
//                 .withTag(APP_TAG, APP_VALUE)
//                 .withTag("Environment", "managed")
//                 .create();

//             log.info("[AzureProvisioningService] Resource Group '{}' created.", rgName);

//             // Step 2: Define and create the VM
//             // The fluent builder creates the NIC, public IP, and OS disk automatically.
//             VirtualMachine vm = computeManager.virtualMachines()
//                 .define(vmName)
//                 .withRegion(region)
//                 .withExistingResourceGroup(rgName)
//                 .withNewPrimaryNetwork("10.0.0.0/24")
//                 .withPrimaryPrivateIPAddressDynamic()
//                 .withNewPrimaryPublicIPAddress(vmName + "-ip")
//                 .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_18_04_LTS)
//                 .withRootUsername(VM_ADMIN_USER)
//                 .withRootPassword(VM_ADMIN_PASSWORD)
//                 .withSize(VirtualMachineSizeTypes.fromString(vmSize))
//                 .withTag(MANAGED_BY_TAG, MANAGED_BY_VALUE)
//                 .withTag(APP_TAG, APP_VALUE)
//                 .withTag("ResourceGroup", rgName)
//                 .create();

//             String vmId = vm.id();
//             log.info("[AzureProvisioningService] Azure VM provisioned: id={}", vmId);

//             return vmId;

//         } catch (com.azure.core.management.exception.ManagementException e) {
//             log.error("[AzureProvisioningService] Azure VM provisioning failed. " +
//                       "Code: {}, Message: {}",
//                       e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
//                       e.getMessage());
//             throw new IllegalStateException(
//                 "Azure VM provisioning failed: " + e.getMessage()
//             );
//         } catch (Exception e) {
//             log.error("[AzureProvisioningService] Unexpected error during Azure VM provisioning: {}",
//                 e.getMessage());
//             throw new IllegalStateException(
//                 "Azure VM provisioning encountered an unexpected error: " + e.getMessage()
//             );
//         }
//     }

//     // ── Teardown Azure VM ─────────────────────────────────────────────────────

//     /**
//      * Destroys the VM's containing Resource Group, which cascades to delete
//      * all associated resources (NIC, disk, public IP, VNet) atomically.
//      *
//      * <p>The Resource Group name is extracted from the VM's resource ID path
//      * since each MindOps deployment creates one dedicated group per deployment.</p>
//      *
//      * @param vmResourceId the Azure VM resource ID stored in {@code provisionedResourcesJson}
//      * @param credentials  decrypted Azure credentials
//      * @throws IllegalArgumentException if the resource ID format is unexpected
//      * @throws IllegalStateException    if the Azure SDK rejects the deletion
//      */
//     public void teardownVm(String vmResourceId, DecryptedAzureCredential credentials) {
//         String resourceGroupName = extractResourceGroupFromId(vmResourceId);

//         log.info("[AzureProvisioningService] Deleting Resource Group '{}' (cascades to VM and all " +
//                  "associated resources).", resourceGroupName);

//         try {
//             TokenCredential tokenCredential = buildTokenCredential(credentials);
//             AzureProfile    azureProfile    = buildAzureProfile(credentials);

//             ResourceManager resourceManager = ResourceManager
//                 .authenticate(tokenCredential, azureProfile)
//                 .withSubscription(credentials.subscriptionId());

//             if (!resourceManager.resourceGroups().contain(resourceGroupName)) {
//                 log.warn("[AzureProvisioningService] Resource Group '{}' not found — " +
//                          "it may have already been deleted.", resourceGroupName);
//                 return;
//             }

//             resourceManager.resourceGroups().deleteByName(resourceGroupName);

//             log.info("[AzureProvisioningService] Resource Group '{}' and all contained " +
//                      "resources successfully deleted.", resourceGroupName);

//         } catch (com.azure.core.management.exception.ManagementException e) {
//             log.error("[AzureProvisioningService] Azure teardown failed. Code: {}, Message: {}",
//                 e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
//                 e.getMessage());
//             throw new IllegalStateException(
//                 "Azure VM teardown failed: " + e.getMessage()
//             );
//         } catch (Exception e) {
//             log.error("[AzureProvisioningService] Unexpected error during Azure teardown: {}",
//                 e.getMessage());
//             throw new IllegalStateException(
//                 "Azure VM teardown encountered an unexpected error: " + e.getMessage()
//             );
//         }
//     }

//     // ── Internal helpers ──────────────────────────────────────────────────────

//     private TokenCredential buildTokenCredential(DecryptedAzureCredential credentials) {
//         return new ClientSecretCredentialBuilder()
//             .tenantId(credentials.tenantId())
//             .clientId(credentials.clientId())
//             .clientSecret(credentials.clientSecret())
//             .build();
//     }

//     private AzureProfile buildAzureProfile(DecryptedAzureCredential credentials) {
//         return new AzureProfile(
//             credentials.tenantId(),
//             credentials.subscriptionId(),
//             AzureEnvironment.AZURE
//         );
//     }

//     /**
//      * Extracts the Resource Group name from an Azure resource ID.
//      *
//      * <p>Azure resource IDs follow this canonical format:
//      * {@code /subscriptions/{sub}/resourceGroups/{rg}/providers/{ns}/{type}/{name}}
//      * The Resource Group segment is always at index 4 (0-based) when splitting by "/".</p>
//      *
//      * @throws IllegalArgumentException if the resource ID does not match the expected format
//      */
//     private String extractResourceGroupFromId(String resourceId) {
//         if (resourceId == null || resourceId.isBlank()) {
//             throw new IllegalArgumentException(
//                 "Cannot extract Resource Group: VM resource ID is null or blank."
//             );
//         }
//         String[] parts = resourceId.split("/");
//         // Expected: ["", "subscriptions", "{subId}", "resourceGroups", "{rgName}", ...]
//         if (parts.length < 5 || !"resourceGroups".equalsIgnoreCase(parts[3])) {
//             throw new IllegalArgumentException(
//                 "Cannot extract Resource Group from malformed Azure resource ID: " + resourceId
//             );
//         }
//         return parts[4];
//     }

//     private String resolveRegion(JsonNode config) {
//         if (config != null && config.has("region") && !config.get("region").isNull()) {
//             String region = config.get("region").asText();
//             if (!region.isBlank()) return region;
//         }
//         return Region.US_EAST.name();
//     }

//     private String resolveVmSize(JsonNode config) {
//         if (config != null && config.has("instanceType") && !config.get("instanceType").isNull()) {
//             String size = config.get("instanceType").asText();
//             if (!size.isBlank()) return size;
//         }
//         return VirtualMachineSizeTypes.STANDARD_B1S.toString();
//     }
// }

package com.ai.project.service;

import com.ai.project.entity.DeploymentPlan;
import com.ai.project.entity.enums.DeploymentStatus;
import com.ai.project.repository.DeploymentPlanRepository;
import com.ai.project.service.CredentialVaultService.DecryptedAzureCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.Subnet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Azure VM provisioning service for the MindOps platform.
 *
 * <p><strong>Enterprise Landing Zone contract (strictly enforced):</strong>
 * <ul>
 *   <li>Resource Group, VNet, and Subnet are PRE-EXISTING — this service
 *       NEVER creates networking primitives.</li>
 *   <li>No Public IP is assigned — all VMs are private-only.</li>
 *   <li>A dedicated NIC is created per VM and named after the {@code planId}
 *       for traceability.</li>
 *   <li>The NIC and VM are placed in the pre-existing {@code mindops-core-rg}
 *       Resource Group.</li>
 * </ul>
 * </p>
 *
 * <p><strong>State machine transitions managed by this service:</strong>
 * <pre>
 *   EXECUTING ──[provisionVmAsync success]──► SUCCESS
 *   EXECUTING ──[Azure SDK exception]──────► FAILED
 * </pre>
 * The transition to EXECUTING is performed by {@code DeploymentService}
 * before this async method is invoked.</p>
 *
 * <p><strong>Credential lifecycle:</strong> The {@link DecryptedAzureCredential}
 * parameter is a short-lived, in-memory record populated by
 * {@link CredentialVaultService#decryptAzureCredential}. It is never logged,
 * serialised, or stored — it exists only for the duration of this method.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureProvisioningService {

    // ── Injected dependencies ─────────────────────────────────────────────────

    private final DeploymentPlanRepository deploymentPlanRepository;
    private final ObjectMapper             objectMapper;

    // ── Enterprise Landing Zone constants ─────────────────────────────────────

    /** Pre-existing Resource Group that owns all MindOps compute resources. */
    private static final String LANDING_ZONE_RG      = "mindops-core-rg";

    /** Pre-existing Virtual Network in the Landing Zone. */
    private static final String LANDING_ZONE_VNET    = "mindops-vnet";

    /** Pre-existing Subnet within the VNet for VM NIC attachment. */
    private static final String LANDING_ZONE_SUBNET  = "mindops-subnet";

    // ── VM defaults ───────────────────────────────────────────────────────────

    private static final String VM_SIZE              = "Standard_B1s";
    private static final String VM_ADMIN_USER        = "mindopsadmin";

    /**
     * Strong default password meeting Azure complexity requirements.
     * Phase 7: replace with SSH public key injection from a Key Vault secret.
     */
    private static final String VM_ADMIN_PASSWORD    = "M!ndOps2025#Secure";

    /** MindOps platform tag applied to all provisioned resources. */
    private static final String TAG_MANAGED_BY_KEY   = "ManagedBy";
    private static final String TAG_MANAGED_BY_VALUE = "MindOps";
    private static final String TAG_APP_KEY          = "Application";
    private static final String TAG_APP_VALUE        = "MindOpsCloud";

    /** Maximum characters written to progressLogJson per error entry. */
    private static final int    MAX_ERROR_LENGTH     = 500;

    // ── Primary async provisioning method ─────────────────────────────────────

    /**
     * Asynchronously provisions a private Azure Linux VM using the pre-existing
     * Enterprise Landing Zone network infrastructure.
     *
     * <p>This method is executed on the {@code taskExecutor} thread pool.
     * The calling HTTP request has already returned {@code 202 Accepted} before
     * this method begins — all DB state updates here are the only mechanism by
     * which the frontend learns of progress or completion.</p>
     *
     * <p><strong>Provisioning sequence:</strong>
     * <ol>
     *   <li>Build an authenticated {@link AzureResourceManager} client from the
     *       decrypted Service Principal credentials.</li>
     *   <li>Look up the pre-existing VNet and extract the target Subnet reference.</li>
     *   <li>Create a NIC attached to that Subnet (no Public IP assigned).</li>
     *   <li>Create a Linux VM ({@code Standard_B1s}, Ubuntu 18.04 LTS) using
     *       that NIC, tagged with MindOps platform metadata.</li>
     *   <li>On success: write the VM's Azure resource ID to
     *       {@code provisionedResourcesJson}, transition status to
     *       {@code SUCCESS}.</li>
     *   <li>On any exception: sanitize the message, append it to
     *       {@code progressLogJson}, transition status to {@code FAILED}.</li>
     * </ol>
     * </p>
     *
     * @param planPublicId the MindOps deployment plan ID (e.g. {@code plan_abc123})
     *                     — used for VM and NIC naming and DB lookups
     * @param configNode   the validated Gemini JSON config node — used to resolve
     *                     optional overrides such as {@code region}
     * @param credentials  decrypted Azure Service Principal credentials
     *                     — held in memory only, never logged or persisted
     */
    @Async("taskExecutor")
    @Transactional
    public void provisionVmAsync(
        String                   planPublicId,
        JsonNode                 configNode,
        DecryptedAzureCredential credentials
    ) {
        log.info("[AzureProvisioningService] Async provisioning started for plan: {}",
            planPublicId);

        DeploymentPlan plan = loadPlan(planPublicId);
        if (plan == null) return;

        try {
            // ── Step 1: Authenticate with Azure ──────────────────────────────
            appendProgress(plan, "Authenticating with Azure via Service Principal…");

            AzureResourceManager azureRM = buildAzureResourceManager(credentials);

            appendProgress(plan, "Azure authentication successful.");

            // ── Step 2: Resolve the Landing Zone VNet and Subnet ─────────────
            appendProgress(plan, String.format(
                "Looking up Landing Zone VNet '%s' in Resource Group '%s'…",
                LANDING_ZONE_VNET, LANDING_ZONE_RG
            ));

            Network vnet = resolveVnet(azureRM, credentials.subscriptionId());

            Subnet subnet = resolveSubnet(vnet);

            appendProgress(plan, String.format(
                "Subnet '%s' resolved. CIDR: %s",
                subnet.name(), subnet.addressPrefix()
            ));

            // ── Step 3: Create a dedicated NIC (no Public IP) ─────────────────
            String nicName = "mindops-nic-" + planPublicId;
            appendProgress(plan, String.format(
                "Creating Network Interface '%s' (private only — no Public IP)…",
                nicName
            ));

            NetworkInterface nic = createNetworkInterface(
                azureRM, nicName, subnet, resolveRegion(configNode)
            );

            appendProgress(plan, String.format(
                "NIC '%s' created. Private IP: %s",
                nic.name(),
                nic.primaryPrivateIP() != null ? nic.primaryPrivateIP() : "DHCP-pending"
            ));

            // ── Step 4: Create the Linux VM ───────────────────────────────────
            // String vmName = "mindops-vm-" + planPublicId;

            // Azure Linux hostnames cannot contain underscores. Strip them out.
            // String safePlanId = planPublicId.replace("_", "");
            // String vmName = "mindops-vm-" + safePlanId;
            // String vmSize = resolveVmSize(configNode);

            // appendProgress(plan, String.format(
            //     "Creating Linux VM '%s' (size: %s, image: Ubuntu 18.04 LTS)…",
            //     vmName, vmSize
            // ));

            // VirtualMachine vm = createVirtualMachine(
            //     azureRM, vmName, vmSize, nic, resolveRegion(configNode), planPublicId
            // );


            // ── Step 4: Create the Linux VM ───────────────────────────────────
            // Azure Linux hostnames cannot contain underscores. Strip them out.
            String safePlanId = planPublicId.replace("_", "");
            String vmName = "mindopsvm" + safePlanId;
            String vmSize = resolveVmSize(configNode);

            appendProgress(plan, String.format(
                "Creating Linux VM '%s' (size: %s, image: Ubuntu 18.04 LTS)…",
                vmName, vmSize
            ));

            VirtualMachine vm = createVirtualMachine(
                azureRM, vmName, vmSize, nic, resolveRegion(configNode), planPublicId
            );

            String vmResourceId = vm.id();

            appendProgress(plan, String.format(
                "VM '%s' provisioned. Azure Resource ID: %s",
                vm.name(), vmResourceId
            ));

            // ── Step 5: Persist SUCCESS state ─────────────────────────────────
            String provisionedJson = buildProvisionedResourcesJson(
                vmResourceId, nic.id(), vm.name(), nic.name(), resolveRegion(configNode)
            );

            appendProgress(plan, "Deployment completed with status: SUCCESS.");

            plan.setProvisionedResourcesJson(provisionedJson);
            plan.setStatus(DeploymentStatus.SUCCESS);
            plan.setCompletedAt(LocalDateTime.now());
            deploymentPlanRepository.save(plan);

            log.info("[AzureProvisioningService] Plan '{}' → SUCCESS. VM: {}",
                planPublicId, vmResourceId);

        } catch (com.azure.core.management.exception.ManagementException e) {
            String sanitized = sanitize(String.format(
                "Azure Management error [%s]: %s",
                e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
                e.getMessage()
            ));
            log.error("[AzureProvisioningService] ManagementException for plan '{}': {}",
                planPublicId, sanitized);
            failPlan(plan, sanitized);

        } catch (com.azure.core.exception.HttpResponseException e) {
            String sanitized = sanitize(String.format(
                "Azure HTTP error [%d]: %s",
                e.getResponse() != null ? e.getResponse().getStatusCode() : 0,
                e.getMessage()
            ));
            log.error("[AzureProvisioningService] HttpResponseException for plan '{}': {}",
                planPublicId, sanitized);
            failPlan(plan, sanitized);

        } catch (IllegalStateException e) {
            // Thrown by our own resolution helpers (VNet/Subnet not found, etc.)
            String sanitized = sanitize(e.getMessage());
            log.error("[AzureProvisioningService] Precondition failure for plan '{}': {}",
                planPublicId, sanitized);
            failPlan(plan, sanitized);

        } catch (Exception e) {
            String sanitized = sanitize("Unexpected error during provisioning: " + e.getMessage());
            log.error("[AzureProvisioningService] Unexpected exception for plan '{}': {}",
                planPublicId, e.getMessage(), e);
            failPlan(plan, sanitized);
        }
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    /**
     * Deletes a VM and its associated NIC, both of which reside in the
     * pre-existing Landing Zone Resource Group.
     *
     * <p><strong>Teardown order:</strong> VM first, then NIC — the VM must be
     * fully deleted before Azure releases the NIC's reservation on the subnet.</p>
     *
     * <p>The Resource Group itself is NOT deleted — it is a shared Landing Zone
     * resource that must persist across all deployments.</p>
     *
     * @param vmResourceId  the Azure VM resource ID stored in
     *                      {@code provisionedResourcesJson} at provision time
     * @param nicResourceId the Azure NIC resource ID (also stored in the JSON)
     * @param credentials   decrypted Azure credentials for this operation
     * @throws IllegalStateException if the Azure API rejects the delete operation
     */
    @Async("taskExecutor")
    @Transactional
    public void teardownVmAsync(
        String                   planPublicId,
        String                   vmResourceId,
        String                   nicResourceId,
        DecryptedAzureCredential credentials
    ) {
        log.info("[AzureProvisioningService] Async teardown started for plan: {}",
            planPublicId);

        DeploymentPlan plan = loadPlan(planPublicId);
        if (plan == null) return;

        try {
            AzureResourceManager azureRM = buildAzureResourceManager(credentials);

            // ── Delete VM ─────────────────────────────────────────────────────
            appendProgress(plan, "Deleting Azure VM: " + vmResourceId);

            if (azureRM.virtualMachines().getById(vmResourceId) != null) {
                azureRM.virtualMachines().deleteById(vmResourceId);
                appendProgress(plan, "VM deleted successfully.");
            } else {
                appendProgress(plan, "VM not found (may have been deleted manually) — skipping.");
            }

            // ── Delete NIC ────────────────────────────────────────────────────
            if (nicResourceId != null && !nicResourceId.isBlank()) {
                appendProgress(plan, "Deleting Network Interface: " + nicResourceId);

                if (azureRM.networkInterfaces().getById(nicResourceId) != null) {
                    azureRM.networkInterfaces().deleteById(nicResourceId);
                    appendProgress(plan, "NIC deleted successfully.");
                } else {
                    appendProgress(plan, "NIC not found (may have been deleted manually) — skipping.");
                }
            }

            appendProgress(plan, "Teardown completed with status: DESTROYED.");

            plan.setStatus(DeploymentStatus.DESTROYED);
            plan.setDestroyedAt(LocalDateTime.now());
            deploymentPlanRepository.save(plan);

            log.info("[AzureProvisioningService] Plan '{}' → DESTROYED.", planPublicId);

        } catch (com.azure.core.management.exception.ManagementException e) {
            String sanitized = sanitize(String.format(
                "Azure teardown Management error [%s]: %s",
                e.getValue() != null ? e.getValue().getCode() : "UNKNOWN",
                e.getMessage()
            ));
            log.error("[AzureProvisioningService] Teardown ManagementException for plan '{}': {}",
                planPublicId, sanitized);
            destroyFailPlan(plan, sanitized);

        } catch (Exception e) {
            String sanitized = sanitize("Unexpected teardown error: " + e.getMessage());
            log.error("[AzureProvisioningService] Unexpected teardown exception for plan '{}': {}",
                planPublicId, e.getMessage(), e);
            destroyFailPlan(plan, sanitized);
        }
    }

    // ── Azure client construction ─────────────────────────────────────────────

    /**
     * Constructs a fully-authenticated {@link AzureResourceManager} from the
     * decrypted Service Principal credentials.
     *
     * <p>The {@link TokenCredential} is not cached — a new instance is created
     * per provisioning call. This ensures credentials from different users are
     * never mixed across async threads.</p>
     *
     * @throws IllegalStateException if Azure authentication fails at SDK init time
     */
    private AzureResourceManager buildAzureResourceManager(DecryptedAzureCredential credentials) {
        try {
            TokenCredential tokenCredential = new ClientSecretCredentialBuilder()
                .tenantId(credentials.tenantId())
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .build();

            AzureProfile azureProfile = new AzureProfile(
                credentials.tenantId(),
                credentials.subscriptionId(),
                AzureEnvironment.AZURE
            );

            return AzureResourceManager
                .authenticate(tokenCredential, azureProfile)
                .withSubscription(credentials.subscriptionId());

        } catch (Exception e) {
            log.error("[AzureProvisioningService] Failed to build AzureResourceManager: {}",
                e.getMessage());
            throw new IllegalStateException(
                "Azure authentication failed. Verify that the Service Principal credentials " +
                "are valid and have Contributor access on the target subscription: " + e.getMessage()
            );
        }
    }

    // ── Landing Zone resource resolution ─────────────────────────────────────

    /**
     * Fetches the pre-existing Landing Zone VNet by name from the
     * {@code mindops-core-rg} Resource Group.
     *
     * <p>This service deliberately does NOT create any VNet — the Enterprise
     * Landing Zone pattern mandates that network topology is provisioned and
     * managed independently of application deployments.</p>
     *
     * @throws IllegalStateException if the VNet does not exist (misconfigured
     *         Landing Zone) or the Service Principal lacks read access
     */
    private Network resolveVnet(AzureResourceManager azureRM, String subscriptionId) {
        try {
            Network vnet = azureRM.networks()
                .getByResourceGroup(LANDING_ZONE_RG, LANDING_ZONE_VNET);

            if (vnet == null) {
                throw new IllegalStateException(String.format(
                    "Landing Zone VNet '%s' not found in Resource Group '%s'. " +
                    "Ensure the Enterprise Landing Zone has been provisioned and that " +
                    "the Service Principal has at least Reader access on subscription '%s'.",
                    LANDING_ZONE_VNET, LANDING_ZONE_RG, subscriptionId
                ));
            }

            log.debug("[AzureProvisioningService] Resolved VNet '{}'. Subnets: {}",
                vnet.name(), vnet.subnets().keySet());

            return vnet;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to resolve Landing Zone VNet '" + LANDING_ZONE_VNET + "': " + e.getMessage()
            );
        }
    }

    /**
     * Extracts the target Subnet from the resolved VNet.
     *
     * <p>Subnets are keyed by name in the Azure SDK's map. The lookup is
     * case-sensitive — the subnet name must match {@link #LANDING_ZONE_SUBNET}
     * exactly as configured in the Landing Zone.</p>
     *
     * @throws IllegalStateException if the Subnet does not exist in the VNet
     */
    private Subnet resolveSubnet(Network vnet) {
        Subnet subnet = vnet.subnets().get(LANDING_ZONE_SUBNET);

        if (subnet == null) {
            throw new IllegalStateException(String.format(
                "Subnet '%s' not found in VNet '%s'. " +
                "Available subnets: [%s]. " +
                "Verify the Landing Zone subnet name configuration.",
                LANDING_ZONE_SUBNET,
                vnet.name(),
                String.join(", ", vnet.subnets().keySet())
            ));
        }

        return subnet;
    }

    // ── NIC creation ─────────────────────────────────────────────────────────

    /**
     * Creates a Network Interface Card (NIC) attached to the Landing Zone
     * Subnet with a dynamic private IP allocation.
     *
     * <p><strong>No Public IP is created or associated.</strong> This is the
     * core Landing Zone security constraint — all VMs are reachable only via
     * internal routing (VPN Gateway, ExpressRoute, or Azure Bastion).</p>
     *
     * <p>The NIC is placed in the pre-existing {@code mindops-core-rg} Resource
     * Group alongside the VM it serves.</p>
     *
     * @param azureRM the authenticated Resource Manager client
     * @param nicName the NIC name — format: {@code mindops-nic-<planId>}
     * @param subnet  the resolved Landing Zone Subnet
     * @param region  the Azure region for NIC placement
     * @return the created {@link NetworkInterface}
     */
    private NetworkInterface createNetworkInterface(
        AzureResourceManager azureRM,
        String               nicName,
        Subnet               subnet,
        String               region
    ) {
        log.debug("[AzureProvisioningService] Creating NIC '{}' in subnet '{}' (no Public IP).",
            nicName, subnet.name());

        return azureRM.networkInterfaces()
            .define(nicName)
            .withRegion(region)
            .withExistingResourceGroup(LANDING_ZONE_RG)
            .withExistingPrimaryNetwork(
                azureRM.networks().getByResourceGroup(LANDING_ZONE_RG, LANDING_ZONE_VNET)
            )
            .withSubnet(subnet.name())
            .withPrimaryPrivateIPAddressDynamic()
            // withPrimaryPublicIPAddress() is intentionally omitted — private-only contract
            .withTag(TAG_MANAGED_BY_KEY, TAG_MANAGED_BY_VALUE)
            .withTag(TAG_APP_KEY,        TAG_APP_VALUE)
            .create();
    }

    // ── VM creation ───────────────────────────────────────────────────────────

    /**
     * Creates a Linux Virtual Machine using the supplied NIC, tagged for
     * MindOps platform management.
     *
     * <p>Image: Ubuntu Server 18.04 LTS (stable, widely available in all regions).
     * Phase 7 can expose image selection via the Gemini config JSON.</p>
     *
     * <p>OS disk is a managed Premium SSD by default (Azure SDK default for
     * Standard_B1s). No additional data disks are attached in the MVP.</p>
     *
     * @param azureRM   the authenticated Resource Manager client
     * @param vmName    VM name — format: {@code mindops-vm-<planId>}
     * @param vmSize    VM size string (e.g. {@code Standard_B1s})
     * @param nic       the pre-created NIC to attach as the primary NIC
     * @param region    the Azure region for VM placement
     * @param planId    the MindOps plan ID — written as a resource tag for traceability
     * @return the created {@link VirtualMachine}
     */
    private VirtualMachine createVirtualMachine(
        AzureResourceManager azureRM,
        String               vmName,
        String               vmSize,
        NetworkInterface     nic,
        String               region,
        String               planId
    ) {
        log.debug("[AzureProvisioningService] Defining VM '{}' — size: {}, region: {}, NIC: {}",
            vmName, vmSize, region, nic.name());

        return azureRM.virtualMachines()
            .define(vmName)
            .withRegion(region)
            .withExistingResourceGroup(LANDING_ZONE_RG)
            .withExistingPrimaryNetworkInterface(nic)
            .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_18_04_LTS)
            .withRootUsername(VM_ADMIN_USER)
            .withRootPassword(VM_ADMIN_PASSWORD)
            // .withComputerName(vmName)
            .withComputerName(vmName.length() > 64 ? vmName.substring(0, 64) : vmName)
            .withSize(VirtualMachineSizeTypes.fromString(vmSize))
            .withTag(TAG_MANAGED_BY_KEY, TAG_MANAGED_BY_VALUE)
            .withTag(TAG_APP_KEY,        TAG_APP_VALUE)
            .withTag("MindOpsPlanId",    planId)
            .withTag("Environment",      "managed")
            .create();
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    /**
     * Constructs the {@code provisionedResourcesJson} array persisted on
     * {@code SUCCESS}. This JSON is the teardown service's source of truth —
     * both the VM resource ID and NIC resource ID are stored so teardown can
     * delete them in the correct order (VM first, then NIC).
     *
     * <p>Format:
     * <pre>
     * [
     *   { "resourceType": "AZURE_VM",  "resourceId": "/subscriptions/…/virtualMachines/…",
     *     "name": "mindops-vm-plan_xxx", "region": "eastus", "provisionedAt": "…" },
     *   { "resourceType": "AZURE_NIC", "resourceId": "/subscriptions/…/networkInterfaces/…",
     *     "name": "mindops-nic-plan_xxx", "region": "eastus", "provisionedAt": "…" }
     * ]
     * </pre>
     * </p>
     */
    private String buildProvisionedResourcesJson(
        String vmResourceId,
        String nicResourceId,
        String vmName,
        String nicName,
        String region
    ) throws JsonProcessingException {
        String timestamp = LocalDateTime.now().toString();
        ArrayNode array  = objectMapper.createArrayNode();

        ObjectNode vmEntry = objectMapper.createObjectNode();
        vmEntry.put("resourceType",   "AZURE_VM");
        vmEntry.put("resourceId",     vmResourceId);
        vmEntry.put("name",           vmName);
        vmEntry.put("region",         region);
        vmEntry.put("resourceGroup",  LANDING_ZONE_RG);
        vmEntry.put("provisionedAt",  timestamp);
        array.add(vmEntry);

        ObjectNode nicEntry = objectMapper.createObjectNode();
        nicEntry.put("resourceType",  "AZURE_NIC");
        nicEntry.put("resourceId",    nicResourceId);
        nicEntry.put("name",          nicName);
        nicEntry.put("region",        region);
        nicEntry.put("resourceGroup", LANDING_ZONE_RG);
        nicEntry.put("provisionedAt", timestamp);
        array.add(nicEntry);

        return objectMapper.writeValueAsString(array);
    }

    // ── Progress log helpers ──────────────────────────────────────────────────

    /**
     * Atomically appends a timestamped progress entry to the plan's
     * {@code progressLogJson} array and persists the update.
     *
     * <p>Each entry follows the shape:
     * {@code {"timestamp": "2025-10-15T10:05:01", "step": "..."}}</p>
     *
     * <p>Failures in this method (e.g. JSON serialisation error) are caught and
     * logged as warnings — a progress log write failure must never abort the
     * provisioning pipeline itself.</p>
     *
     * @param plan the managed {@link DeploymentPlan} entity
     * @param step human-readable description of the current execution step
     */
    private void appendProgress(DeploymentPlan plan, String step) {
        try {
            ArrayNode logArray;
            String existing = plan.getProgressLogJson();

            if (existing == null || existing.isBlank() || existing.equals("[]")) {
                logArray = objectMapper.createArrayNode();
            } else {
                logArray = (ArrayNode) objectMapper.readTree(existing);
            }

            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("timestamp", LocalDateTime.now().toString());
            entry.put("step",      step);
            logArray.add(entry);

            plan.setProgressLogJson(objectMapper.writeValueAsString(logArray));
            deploymentPlanRepository.save(plan);

            log.debug("[AzureProvisioningService] Progress [{}]: {}", plan.getPublicId(), step);

        } catch (JsonProcessingException e) {
            log.warn("[AzureProvisioningService] Failed to append progress entry for plan '{}': {}",
                plan.getPublicId(), e.getMessage());
        }
    }

    // ── Terminal state helpers ────────────────────────────────────────────────

    /**
     * Transitions the plan to {@code FAILED} and persists a sanitized error
     * message into the progress log. Called in all exception catch blocks.
     *
     * <p>Uses a raw {@link DeploymentPlanRepository} save (no service layer
     * re-entrant call) to avoid circular dependency issues between the
     * async thread and the Spring transaction context.</p>
     */
    private void failPlan(DeploymentPlan plan, String sanitizedError) {
        try {
            appendProgress(plan, "FAILED: " + sanitizedError);
            plan.setStatus(DeploymentStatus.FAILED);
            plan.setCompletedAt(LocalDateTime.now());
            deploymentPlanRepository.save(plan);
            log.warn("[AzureProvisioningService] Plan '{}' → FAILED: {}", plan.getPublicId(), sanitizedError);
        } catch (Exception e) {
            log.error("[AzureProvisioningService] Could not persist FAILED state for plan '{}': {}",
                plan.getPublicId(), e.getMessage(), e);
        }
    }

    /**
     * Transitions the plan to {@code DESTROY_FAILED} and persists a sanitized
     * error message. Called in teardown exception catch blocks.
     */
    private void destroyFailPlan(DeploymentPlan plan, String sanitizedError) {
        try {
            appendProgress(plan, "DESTROY_FAILED: " + sanitizedError);
            plan.setStatus(DeploymentStatus.DESTROY_FAILED);
            plan.setCompletedAt(LocalDateTime.now());
            deploymentPlanRepository.save(plan);
            log.warn("[AzureProvisioningService] Plan '{}' → DESTROY_FAILED: {}",
                plan.getPublicId(), sanitizedError);
        } catch (Exception e) {
            log.error("[AzureProvisioningService] Could not persist DESTROY_FAILED state for plan '{}': {}",
                plan.getPublicId(), e.getMessage(), e);
        }
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    /**
     * Loads a {@link DeploymentPlan} by its public ID.
     * Returns {@code null} and logs an error if the plan is not found —
     * callers should exit immediately on a {@code null} return to avoid
     * NPEs in the async thread with no meaningful plan to update.
     */
    private DeploymentPlan loadPlan(String planPublicId) {
        Optional<DeploymentPlan> planOpt = deploymentPlanRepository.findByPublicId(planPublicId);
        if (planOpt.isEmpty()) {
            log.error("[AzureProvisioningService] Plan '{}' not found in DB at start of async thread. " +
                      "Aborting provisioning.", planPublicId);
            return null;
        }
        return planOpt.get();
    }

    /**
     * Sanitizes a raw exception message before writing it to the database or logs.
     *
     * <p>Removes Azure SDK package references (e.g.
     * {@code com.azure.resourcemanager.compute.*}) that could expose internal
     * implementation details, and truncates to {@link #MAX_ERROR_LENGTH} characters
     * to prevent unbounded {@code progressLogJson} growth.</p>
     *
     * @param raw the raw exception message string
     * @return a sanitized, length-bounded error string safe for DB persistence
     */
    private String sanitize(String raw) {
        if (raw == null) return "An unexpected error occurred.";
        String cleaned = raw.replaceAll(
            "(com\\.azure\\.[\\w.]+|com\\.microsoft\\.[\\w.]+):",
            "Azure SDK error:"
        );
        return cleaned.length() > MAX_ERROR_LENGTH
            ? cleaned.substring(0, MAX_ERROR_LENGTH) + "…"
            : cleaned;
    }

    /**
     * Resolves the target Azure region from the Gemini config JSON.
     * Falls back to {@code eastus} if the field is absent or blank.
     *
     * <p>Region names are normalised to lowercase-no-spaces ARM format
     * (e.g. {@code "East US"} → {@code "eastus"}).</p>
     */
    private String resolveRegion(JsonNode configNode) {
        if (configNode != null && configNode.has("region") && !configNode.get("region").isNull()) {
            String region = configNode.get("region").asText("").trim();
            if (!region.isEmpty()) {
                return region.toLowerCase().replaceAll("\\s+", "");
            }
        }
        return Region.US_EAST.name();
    }

    /**
     * Resolves the VM size from the Gemini config JSON.
     * Falls back to {@link #VM_SIZE} ({@code Standard_B1s}) if absent.
     *
     * <p>The value is used directly as a {@link VirtualMachineSizeTypes} string.
     * If Gemini returns an invalid size, the Azure SDK will throw a
     * {@code ManagementException} which is caught and handled in the caller.</p>
     */
    private String resolveVmSize(JsonNode configNode) {
        if (configNode != null && configNode.has("instanceType")
                && !configNode.get("instanceType").isNull()) {
            String size = configNode.get("instanceType").asText("").trim();
            if (!size.isEmpty()) return size;
        }
        return VM_SIZE;
    }
}