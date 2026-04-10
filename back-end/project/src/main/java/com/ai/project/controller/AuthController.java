package com.ai.project.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.project.dto.AuthResponse;
import com.ai.project.dto.LoginRequest;
import com.ai.project.dto.PasswordChangeRequest;
import com.ai.project.dto.RefreshRequest;
import com.ai.project.dto.RegisterRequest;
import com.ai.project.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for all authentication lifecycle operations.
 *
 * <p>Error handling strategy: service-layer exceptions are caught here and
 * translated into structured JSON responses. A global {@code @ControllerAdvice}
 * will be added in a later phase to centralise this — for now, explicit
 * try/catch blocks here keep the phase self-contained and auditable.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService=new AuthService();

    // ── POST /api/v1/auth/register ───────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            if ("EMAIL_ALREADY_EXISTS".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error",   "EMAIL_ALREADY_EXISTS",
                    "message", "An account with this email already exists."
                ));
            }
            log.error("[AuthController] Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   "REGISTRATION_FAILED",
                "message", "Registration could not be completed. Please try again."
            ));
        }
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error",   "INVALID_CREDENTIALS",
                "message", "Invalid email or password."
            ));
        } catch (DisabledException | LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error",   "ACCOUNT_DISABLED",
                "message", "This account has been deactivated. Contact support."
            ));
        }
    }

    // ── POST /api/v1/auth/refresh ────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            AuthResponse response = authService.refresh(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            String code = e.getMessage();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error",   code,
                "message", resolveRefreshErrorMessage(code)
            ));
        }
    }

    // ── POST /api/v1/auth/logout ─────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
        @Valid @RequestBody RefreshRequest request,
        HttpServletRequest httpRequest
    ) {
        String rawAuthHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(rawAuthHeader) || !rawAuthHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error",   "MISSING_TOKEN",
                "message", "Authorization header with Bearer token is required."
            ));
        }

        String accessToken = rawAuthHeader.substring(7).trim();

        try {
            AuthResponse response = authService.logout(accessToken, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AuthController] Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   "LOGOUT_FAILED",
                "message", "Session termination failed. Please try again."
            ));
        }
    }

    // ── PATCH /api/v1/auth/password ──────────────────────────────────────────

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            AuthResponse response = authService.changePassword(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error",   "CURRENT_PASSWORD_INCORRECT",
                "message", "The current password you provided is incorrect."
            ));
        } catch (IllegalArgumentException e) {
            if ("NEW_PASSWORD_SAME_AS_CURRENT".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error",   "NEW_PASSWORD_SAME_AS_CURRENT",
                    "message", "New password must be different from the current password."
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error",   "PASSWORD_CHANGE_FAILED",
                "message", "Password change request is invalid."
            ));
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private String resolveRefreshErrorMessage(String code) {
        return switch (code) {
            case "REFRESH_TOKEN_INVALID"       -> "Refresh token is invalid or has expired. Please log in again.";
            case "REFRESH_TOKEN_REVOKED"        -> "This refresh token has been revoked. Please log in again.";
            case "USER_NOT_FOUND_OR_INACTIVE"  -> "Associated account not found or has been deactivated.";
            default                            -> "Token refresh failed. Please log in again.";
        };
    }
}