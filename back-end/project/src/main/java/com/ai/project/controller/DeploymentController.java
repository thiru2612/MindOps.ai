// package com.ai.project.controller;

// import com.ai.project.dto.DeploymentPlanResponse;
// import com.ai.project.dto.GeneratePlanRequest;
// import com.ai.project.dto.PagedResponse;
// import com.ai.project.entity.enums.DeploymentStatus;
// import com.ai.project.service.DeploymentService;
// import jakarta.validation.Valid;
// import jakarta.validation.constraints.Max;
// import jakarta.validation.constraints.Min;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// /**
//  * REST controller for the deployment orchestration state machine.
//  *
//  * <p>Rate limiting on {@code POST /generate} is enforced by
//  * {@link com.ai.project.config.RateLimitInterceptor} — registered via
//  * {@link com.ai.project.config.WebMvcConfig} to intercept exactly this path.
//  * The controller itself has no rate-limit awareness; the interceptor handles
//  * it transparently before the controller method is invoked.</p>
//  *
//  * <p>All endpoints require {@code ROLE_USER} or {@code ROLE_ADMIN}.
//  * Ownership enforcement (ensuring a user can only see/execute their own plans)
//  * is handled inside {@link DeploymentService} via the authenticated principal.</p>
//  */
// @Slf4j
// @Validated
// @RestController
// @RequestMapping("/api/v1/deployments")
// @RequiredArgsConstructor
// public class DeploymentController {

//     private final DeploymentService deploymentService;

//     // ── POST /api/v1/deployments/generate ────────────────────────────────────
//     // Rate-limited by RateLimitInterceptor (10 req/hr/user via Bucket4j)

//     @PostMapping("/generate")
//     @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//     public ResponseEntity<DeploymentPlanResponse> generatePlan(
//         @Valid @RequestBody GeneratePlanRequest request
//     ) {
//         DeploymentPlanResponse response = deploymentService.generatePlan(request);
//         return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
//     }

//     // ── POST /api/v1/deployments/{planId}/execute ────────────────────────────

//     @PostMapping("/{planId}/execute")
//     @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//     public ResponseEntity<Map<String, Object>> approveAndExecutePlan(
//         @PathVariable String planId
//     ) {
//         DeploymentPlanResponse response = deploymentService.approveAndExecutePlan(planId);
//         return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
//             "deploymentPlanId", response.getPlanId(),
//             "status",           response.getStatus(),
//             "message",          "Deployment execution has been triggered. " +
//                                 "Poll GET /api/v1/deployments/" + planId + " for live status.",
//             "executionStarted", response.getUpdatedAt()
//         ));
//     }

//     // ── GET /api/v1/deployments ──────────────────────────────────────────────

//     @GetMapping
//     @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//     public ResponseEntity<PagedResponse<DeploymentPlanResponse>> listDeployments(
//         @RequestParam(defaultValue = "0")  @Min(0)              int page,
//         @RequestParam(defaultValue = "10") @Min(1) @Max(50)     int size,
//         @RequestParam(required = false)                          DeploymentStatus status
//     ) {
//         return ResponseEntity.ok(
//             deploymentService.listPlansForCurrentUser(page, size, status)
//         );
//     }

//     // ── GET /api/v1/deployments/{planId} ─────────────────────────────────────

//     @GetMapping("/{planId}")
//     @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//     public ResponseEntity<DeploymentPlanResponse> getPlan(
//         @PathVariable String planId
//     ) {
//         return ResponseEntity.ok(deploymentService.getPlanById(planId));
//     }
// }

package com.ai.project.controller;

import com.ai.project.dto.DeploymentPlanResponse;
import com.ai.project.dto.GeneratePlanRequest;
import com.ai.project.dto.PagedResponse;
import com.ai.project.entity.enums.DeploymentStatus;
import com.ai.project.service.DeploymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.time.LocalDateTime;

/**
 * REST controller for the deployment orchestration state machine.
 * Updated in Phase 5 with the {@code DELETE /{planId}} teardown endpoint.
 *
 * <p>Rate limiting on {@code POST /generate} is enforced upstream by
 * {@link com.ai.project.config.RateLimitInterceptor} — the controller
 * has no rate-limit awareness by design.</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    // ── POST /api/v1/deployments/generate ────────────────────────────────────
    // Rate-limited by RateLimitInterceptor (10 req/hr/user via Bucket4j)

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<DeploymentPlanResponse> generatePlan(
        @Valid @RequestBody GeneratePlanRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(deploymentService.generatePlan(request));
    }

    // ── POST /api/v1/deployments/{planId}/execute ────────────────────────────

    @PostMapping("/{planId}/execute")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> approveAndExecutePlan(
        @PathVariable String planId
    ) {
        DeploymentPlanResponse response = deploymentService.approveAndExecutePlan(planId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "deploymentPlanId", response.getPlanId(),
            "status",           response.getStatus(),
            "message",          "Deployment execution has been triggered. " +
                                "Poll GET /api/v1/deployments/" + planId + " for live status.",
            "executionStarted", response.getUpdatedAt() != null
                                    ? response.getUpdatedAt().toString()
                                    : "in progress"
        ));
    }

    // ── GET /api/v1/deployments ──────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<PagedResponse<DeploymentPlanResponse>> listDeployments(
        @RequestParam(defaultValue = "0")  @Min(0)          int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
        @RequestParam(required = false)                      DeploymentStatus status
    ) {
        return ResponseEntity.ok(
            deploymentService.listPlansForCurrentUser(page, size, status)
        );
    }

    // ── GET /api/v1/deployments/{planId} ─────────────────────────────────────

    @GetMapping("/{planId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<DeploymentPlanResponse> getPlan(
        @PathVariable String planId
    ) {
        return ResponseEntity.ok(deploymentService.getPlanById(planId));
    }

    // ── DELETE /api/v1/deployments/{planId} — Teardown ────────────────────────

    /**
     * Initiates controlled teardown of all resources provisioned under the
     * specified deployment plan.
     *
     * <p>Returns {@code 202 Accepted} immediately — the teardown runs asynchronously.
     * Poll {@code GET /api/v1/deployments/{planId}} to observe the transition:
     * {@code DESTROYING → DESTROYED | DESTROY_FAILED}.</p>
     *
     * <p>Preconditions enforced by {@link DeploymentService#teardownPlan}:
     * <ul>
     *   <li>Plan must belong to the authenticated user.</li>
     *   <li>Plan status must be {@code SUCCESS} or {@code FAILED}.</li>
     *   <li>{@code provisionedResourcesJson} must contain at least one resource.</li>
     * </ul>
     * </p>
     */
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> teardownPlan(
        @PathVariable String planId
    ) {
        DeploymentPlanResponse response = deploymentService.teardownPlan(planId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "deploymentPlanId", response.getPlanId(),
            "status",           response.getStatus(),
            "message",          "Teardown initiated. Resources will be deleted. " +
                                "Poll GET /api/v1/deployments/" + planId + " for status.",
            "teardownStarted",  LocalDateTime.now().toString()
        ));
    }
}