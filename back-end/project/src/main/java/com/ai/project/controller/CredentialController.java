package com.ai.project.controller;

import com.ai.project.dto.AwsCredentialRequest;
import com.ai.project.dto.AzureCredentialRequest;
import com.ai.project.dto.CredentialResponse;
import com.ai.project.service.CredentialVaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the credential vault lifecycle.
 *
 * <p>All endpoints require {@code ROLE_USER} or {@code ROLE_ADMIN}.
 * Users can only manage their own credentials — ownership enforcement
 * is handled inside {@link CredentialVaultService} via the authenticated
 * principal, not via path variable user IDs.</p>
 */
@RestController
@RequestMapping("/api/v1/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialVaultService credentialVaultService;

    // ── POST /api/v1/credentials/aws ─────────────────────────────────────────

    @PostMapping("/aws")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<CredentialResponse> storeAwsCredential(
        @Valid @RequestBody AwsCredentialRequest request
    ) {
        CredentialResponse response = credentialVaultService.storeAwsCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /api/v1/credentials/azure ───────────────────────────────────────

    @PostMapping("/azure")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<CredentialResponse> storeAzureCredential(
        @Valid @RequestBody AzureCredentialRequest request
    ) {
        CredentialResponse response = credentialVaultService.storeAzureCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /api/v1/credentials ──────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, List<CredentialResponse>>> listCredentials() {
        List<CredentialResponse> credentials =
            credentialVaultService.listCredentialsForCurrentUser();
        return ResponseEntity.ok(Map.of("credentials", credentials));
    }

    // ── DELETE /api/v1/credentials/{publicId} ────────────────────────────────

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCredential(
        @PathVariable String publicId
    ) {
        credentialVaultService.deleteCredential(publicId);
        return ResponseEntity.ok(Map.of(
            "message", "Credential " + publicId + " deleted successfully."
        ));
    }
}