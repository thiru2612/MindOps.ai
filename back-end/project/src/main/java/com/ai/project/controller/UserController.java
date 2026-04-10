package com.ai.project.controller;

import com.ai.project.dto.PagedResponse;
import com.ai.project.dto.UserProfileResponse;
import com.ai.project.service.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user profile and admin user management operations.
 *
 * <p>RBAC is enforced at two levels:
 * <ol>
 *   <li>URL pattern matching in {@code SecurityConfig} for broad rules.</li>
 *   <li>{@code @PreAuthorize} annotations here for method-level precision,
 *       providing a defence-in-depth approach.</li>
 * </ol>
 * </p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── GET /api/v1/users/me ─────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    // ── GET /api/v1/users ────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getAllUsers(
        @RequestParam(defaultValue = "0")  @Min(0)        int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    // ── DELETE /api/v1/users/{publicId} ──────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateUser(
        @PathVariable String publicId
    ) {
        userService.deactivateUser(publicId);
        return ResponseEntity.ok(Map.of(
            "message", "User " + publicId + " has been deactivated."
        ));
    }
}