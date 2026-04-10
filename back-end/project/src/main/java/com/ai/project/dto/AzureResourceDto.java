package com.ai.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a single Azure resource discovered via the Resource Manager API,
 * enriched with a live cost estimate from the Azure Retail Prices API.
 *
 * <p>{@code estimatedMonthlyCost} is nullable — if the pricing API is
 * unavailable for a specific SKU or resource type, this field is omitted
 * from the JSON response rather than surfacing a misleading zero.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureResourceDto {

    /** Azure fully-qualified resource ID — unique identifier within the subscription. */
    private String resourceId;

    /** Human-readable resource name (e.g. {@code mindops-vm-1718000000000}). */
    private String name;

    /**
     * Azure resource type in provider/type format.
     * Example: {@code Microsoft.Compute/virtualMachines}
     */
    private String type;

    /** Azure region in ARM format. Example: {@code eastus}, {@code westeurope}. */
    private String location;

    /**
     * The VM hardware profile size / SKU name used for pricing lookup.
     * Example: {@code Standard_B1s}, {@code Standard_D2s_v3}.
     * {@code null} for non-compute resources (storage accounts, public IPs, etc.)
     * that do not have a discrete SKU in the Compute namespace.
     */
    private String sku;

    /**
     * Estimated monthly cost in USD, calculated as:
     * {@code retailPrice (hourly) × 730 hours}.
     * {@code null} if the pricing API returned no match for this SKU/region pair.
     */
    private Double estimatedMonthlyCost;
}