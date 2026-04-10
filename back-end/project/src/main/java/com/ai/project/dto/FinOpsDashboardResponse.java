package com.ai.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level FinOps dashboard response aggregating all MindOps-tagged Azure
 * resources under a single credential with their combined cost estimate.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code provider} — always {@code "AZURE"} in Phase 6.</li>
 *   <li>{@code totalMonthlyCost} — sum of all non-null
 *       {@link AzureResourceDto#getEstimatedMonthlyCost()} values. Zero if
 *       no resources are found or all pricing lookups failed.</li>
 *   <li>{@code resourceCount} — total number of tagged resources discovered,
 *       regardless of whether pricing data was available.</li>
 *   <li>{@code pricingCurrency} — always {@code "USD"} (Azure Retail Prices
 *       API returns prices in USD by default).</li>
 *   <li>{@code generatedAt} — server-side timestamp of when the snapshot
 *       was collected. Clients should display this to indicate data freshness.</li>
 *   <li>{@code pricingDisclaimer} — static notice that estimates are based on
 *       public retail prices and may differ from actual billed amounts.</li>
 * </ul>
 * </p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinOpsDashboardResponse {

    private String             provider;
    private Double             totalMonthlyCost;
    private Integer            resourceCount;
    private String             pricingCurrency;
    private List<AzureResourceDto> resources;
    private LocalDateTime      generatedAt;
    private String             pricingDisclaimer;
    private String             credentialId;
}