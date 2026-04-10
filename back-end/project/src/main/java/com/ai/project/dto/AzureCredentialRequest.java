package com.ai.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UUID pattern reused for tenantId, clientId, and subscriptionId.
 * Azure uses standard RFC 4122 UUIDs for all resource identifiers.
 */
@Getter
@Setter
@NoArgsConstructor
public class AzureCredentialRequest {

    private static final String UUID_PATTERN =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @NotBlank(message = "Credential label is required.")
    @Size(max = 100, message = "Credential label must not exceed 100 characters.")
    private String credentialLabel;

    @NotBlank(message = "Azure Tenant ID is required.")
    @Pattern(regexp = UUID_PATTERN, message = "Tenant ID must be a valid UUID.")
    private String tenantId;

    @NotBlank(message = "Azure Client ID is required.")
    @Pattern(regexp = UUID_PATTERN, message = "Client ID must be a valid UUID.")
    private String clientId;

    @NotBlank(message = "Azure Client Secret is required.")
    @Size(min = 8, max = 255, message = "Client Secret appears to be invalid.")
    private String clientSecret;

    @NotBlank(message = "Azure Subscription ID is required.")
    @Pattern(regexp = UUID_PATTERN, message = "Subscription ID must be a valid UUID.")
    private String subscriptionId;
}