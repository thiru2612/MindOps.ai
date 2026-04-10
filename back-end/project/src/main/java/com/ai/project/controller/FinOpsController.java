package com.ai.project.controller;

import com.ai.project.dto.FinOpsDashboardResponse;
import com.ai.project.service.AzureFinOpsService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the FinOps read-only dashboard endpoints.
 *
 * <p>Phase 6 exposes Azure only. AWS FinOps (CloudWatch Cost Explorer
 * integration) is planned for Phase 7.</p>
 *
 * <p>The {@code credentialId} request parameter is validated at the controller
 * level to fail fast before hitting the service layer or decrypting any
 * credentials. The format constraint ({@code cred_[a-zA-Z0-9]{12}}) mirrors
 * the public ID format assigned at credential creation time.</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/finops")
@RequiredArgsConstructor
public class FinOpsController {

    private final AzureFinOpsService azureFinOpsService;

    /**
     * Returns a live FinOps dashboard snapshot for all Azure resources
     * tagged {@code ManagedBy=MindOps} under the specified credential.
     *
     * <p><strong>Latency note:</strong> This endpoint performs N+1 external
     * HTTP calls (1 Azure ARM list + N Azure Retail Prices API calls — one per
     * resource). For subscriptions with many MindOps resources this may take
     * several seconds. Phase 7 can add response caching (e.g. 5-minute TTL via
     * Spring Cache + Caffeine) if latency becomes a concern.</p>
     *
     * @param credentialId the {@code public_id} of a stored Azure credential
     *                     belonging to the authenticated user (required)
     * @return a {@link FinOpsDashboardResponse} containing all discovered resources
     *         and their estimated monthly costs
     */
    @GetMapping("/azure/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<FinOpsDashboardResponse> getAzureDashboard(
        @RequestParam
        @NotBlank(message = "credentialId is required.")
        @Pattern(
            regexp  = "^cred_[a-zA-Z0-9]{12}$",
            message = "credentialId format is invalid. Expected: cred_xxxxxxxxxxxx."
        )
        String credentialId
    ) {
        log.info("[FinOpsController] Azure dashboard requested for credential: {}",
            credentialId);

        FinOpsDashboardResponse dashboard = azureFinOpsService.getLiveDashboard(credentialId);
        return ResponseEntity.ok(dashboard);
    }
}