package com.ai.project.dto;

import com.ai.project.entity.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private String        userId;
    private String        fullName;
    private String        email;
    private Role          role;
    private Boolean       isActive;
    private LocalDateTime createdAt;

    /** Summary of configured cloud providers — shown on own profile only. */
    private CredentialSummary credentialSummary;

    /** Deployment count — shown in admin list view. */
    private Long deploymentCount;

    @Getter
    @Builder
    public static class CredentialSummary {
        private boolean awsConfigured;
        private boolean azureConfigured;
    }
}