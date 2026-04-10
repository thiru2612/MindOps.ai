package com.ai.project.dto;

import com.ai.project.entity.enums.DeploymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Outbound DTO for a {@code DeploymentPlan} entity.
 *
 * <p>{@code @JsonRawValue} on {@code aiGeneratedConfig} instructs Jackson to
 * embed the stored JSON string directly into the response payload as a nested
 * JSON object — not as an escaped string. This preserves the structured nature
 * of the Gemini output without a double-serialization round-trip.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentPlanResponse {

    private String           planId;
    private DeploymentStatus status;
    private String           userPrompt;
    private Double           costEstimate;
    private String           credentialId;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;

    /**
     * The structured JSON object returned by Gemini, embedded as a raw
     * nested JSON value in the response (not a string).
     */
    @JsonRawValue
    private String aiGeneratedConfig;
}