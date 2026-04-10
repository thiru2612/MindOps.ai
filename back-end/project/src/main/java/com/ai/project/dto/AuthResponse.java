package com.ai.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Unified authentication response DTO.
 *
 * <p>Fields are conditionally included in JSON output — {@code @JsonInclude(NON_NULL)}
 * ensures that fields not relevant to a specific operation (e.g. {@code refreshToken}
 * in a token-refresh response) are omitted from the serialized payload, keeping
 * responses minimal and unambiguous.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    // ── Token fields (login and refresh responses) ────────────────────────────
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long   accessTokenExpiresIn;
    private Long   refreshTokenExpiresIn;

    // ── User identity fields (registration and login responses) ───────────────
    private String userId;
    private String email;
    private String fullName;
    private String role;

    // ── Lifecycle timestamps ──────────────────────────────────────────────────
    private LocalDateTime createdAt;

    // ── Generic message (logout, password change, deactivation) ──────────────
    private String message;
}