package com.ai.project.dto;

import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.CredentialValidationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Outbound credential DTO.
 *
 * <p><strong>Security invariant:</strong> This DTO NEVER carries plaintext or
 * raw encrypted credential values. Sensitive fields are always masked before
 * being set on this object. The masking is performed exclusively in
 * {@code CredentialVaultService} using the {@code mask()} utility method.</p>
 *
 * <p>Fields irrelevant to the provider type are omitted via
 * {@code @JsonInclude(NON_NULL)} to keep responses clean.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponse {

    private String                    credentialId;
    private CloudProvider             provider;
    private String                    credentialLabel;
    private CredentialValidationStatus validationStatus;
    private LocalDateTime             createdAt;

    // ── AWS masked fields ────────────────────────────────────────────────────
    /** Example: "AKIA***MPLE" — first 4 and last 4 characters visible. */
    private String accessKeyIdMasked;
    private String defaultRegion;

    // ── Azure masked fields ──────────────────────────────────────────────────
    /** Example: "xxxxxxxx-****-****-****-xxxxxxxxxxxx" */
    private String clientIdMasked;
    /** Example: "zzzzzzzz-****-****-****-zzzzzzzzzzzz" */
    private String subscriptionIdMasked;
}