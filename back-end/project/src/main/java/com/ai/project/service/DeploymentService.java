// // package com.ai.project.service;

// // import com.ai.project.dto.DeploymentPlanResponse;
// // import com.ai.project.dto.GeneratePlanRequest;
// // import com.ai.project.dto.PagedResponse;
// // import com.ai.project.entity.AiComplianceLog;
// // import com.ai.project.entity.CloudCredential;
// // import com.ai.project.entity.DeploymentPlan;
// // import com.ai.project.entity.User;
// // import com.ai.project.entity.enums.CloudProvider;
// // import com.ai.project.entity.enums.DeploymentStatus;
// // import com.ai.project.exception.ResourceNotFoundException;
// // import com.ai.project.repository.AiComplianceLogRepository;
// // import com.ai.project.repository.CloudCredentialRepository;
// // import com.ai.project.repository.DeploymentPlanRepository;
// // import com.fasterxml.jackson.core.JsonProcessingException;
// // import com.fasterxml.jackson.databind.JsonNode;
// // import com.fasterxml.jackson.databind.ObjectMapper;
// // import lombok.RequiredArgsConstructor;
// // import lombok.extern.slf4j.Slf4j;
// // import org.springframework.data.domain.Page;
// // import org.springframework.data.domain.PageRequest;
// // import org.springframework.data.domain.Pageable;
// // import org.springframework.data.domain.Sort;
// // import org.springframework.scheduling.annotation.Async;
// // import org.springframework.stereotype.Service;
// // import org.springframework.transaction.annotation.Transactional;

// // import java.time.LocalDateTime;
// // import java.util.List;

// // /**
// //  * Core deployment state machine service.
// //  *
// //  * <p>State transitions managed by this service:
// //  * <pre>
// //  *   [no entity] ──generatePlan()──► PENDING
// //  *   PENDING ──approveAndExecutePlan()──► EXECUTING ──[Phase 5]──► SUCCESS | FAILED
// //  * </pre>
// //  * </p>
// //  *
// //  * <p>Design decisions:
// //  * <ul>
// //  *   <li>{@code generatePlan} saves the compliance log unconditionally —
// //  *       even if Gemini fails or the guardrail fires, the attempt is recorded.
// //  *       The {@code deploymentPlanId} FK is nullable for this reason.</li>
// //  *   <li>The actual cloud SDK execution in {@code executeCloudDeployment} is an
// //  *       {@code @Async} method so it does not block the HTTP response thread.
// //  *       The controller returns 202 Accepted immediately after the state
// //  *       transitions to EXECUTING.</li>
// //  *   <li>All state transitions are performed inside dedicated {@code @Transactional}
// //  *       methods to ensure atomicity — a failed SDK call will not leave the plan
// //  *       stuck in EXECUTING without a corresponding FAILED status write.</li>
// //  * </ul>
// //  * </p>
// //  */
// // @Slf4j
// // @Service
// // @RequiredArgsConstructor
// // public class DeploymentService {

// //     private final DeploymentPlanRepository  deploymentPlanRepository;
// //     private final CloudCredentialRepository credentialRepository;
// //     private final AiComplianceLogRepository complianceLogRepository;
// //     private final GeminiClientService       geminiClientService;
// //     private final UserService               userService;
// //     private final ObjectMapper              objectMapper;

// //     // ── Step A/B/C: Generate Plan ────────────────────────────────────────────

// //     /**
// //      * Verifies credential ownership, calls Gemini with the guardrail prompt,
// //      * parses the cost estimate, saves the compliance log and the DeploymentPlan
// //      * with status {@code PENDING}.
// //      *
// //      * @param request the user's generation request (prompt + credentialId)
// //      * @return the saved {@link DeploymentPlanResponse} with status PENDING
// //      * @throws ResourceNotFoundException if the credential does not belong to the user
// //      * @throws IllegalArgumentException  on Gemini guardrail violation or parse failure
// //      * @throws IllegalStateException     if the Gemini API is unavailable
// //      */
// //     @Transactional
// //     public DeploymentPlanResponse generatePlan(GeneratePlanRequest request) {
// //         User currentUser = userService.resolveCurrentUser();

// //         // ── 1. Verify credential ownership ───────────────────────────────────
// //         CloudCredential credential = credentialRepository
// //             .findByPublicIdAndUser(request.getCredentialId(), currentUser)
// //             .orElseThrow(() -> new ResourceNotFoundException(
// //                 "CloudCredential", request.getCredentialId()
// //             ));

// //         // ── 2. Call Gemini with system guardrail ──────────────────────────────
// //         GeminiClientService.GeminiResult geminiResult;
// //         try {
// //             geminiResult = geminiClientService.generateInfrastructureConfig(request.getPrompt());
// //         } catch (IllegalArgumentException | IllegalStateException geminiEx) {
// //             // Log the failed attempt before re-throwing — compliance record is mandatory
// //             saveComplianceLog(
// //                 null,
// //                 currentUser,
// //                 request.getPrompt(),
// //                 SYSTEM_PROMPT_PREFIX + request.getPrompt(),
// //                 "FAILED: " + geminiEx.getMessage(),
// //                 null,
// //                 null,
// //                 0,
// //                 credential.getProvider(),
// //                 true,
// //                 geminiEx.getMessage()
// //             );
// //             throw geminiEx;
// //         }

// //         // ── 3. Extract cost estimate from validated JSON ──────────────────────
// //         Double costEstimate = extractCostEstimate(geminiResult.validatedConfigJson());

// //         // ── 4. Persist the DeploymentPlan (status = PENDING) ─────────────────
// //         DeploymentPlan plan = DeploymentPlan.builder()
// //             .user(currentUser)
// //             .credential(credential)
// //             .promptSnapshot(request.getPrompt())
// //             .targetProvider(credential.getProvider())
// //             .status(DeploymentStatus.PENDING)
// //             .sdkParamsJson(geminiResult.validatedConfigJson())
// //             .executionSummaryJson(geminiResult.validatedConfigJson())
// //             .progressLogJson("[]")
// //             .build();

// //         DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

// //         // ── 5. Save compliance log linked to the new plan ─────────────────────
// //         saveComplianceLog(
// //             savedPlan,
// //             currentUser,
// //             request.getPrompt(),
// //             geminiResult.sanitizedPromptSent(),
// //             geminiResult.rawGeminiResponseText(),
// //             geminiResult.totalTokenCount(),
// //             geminiResult.promptTokenCount(),
// //             geminiResult.latencyMs(),
// //             credential.getProvider(),
// //             false,
// //             null
// //         );

// //         log.info("[DeploymentService] Plan '{}' created with status PENDING for user '{}'.",
// //             savedPlan.getPublicId(), currentUser.getEmail());

// //         return toResponse(savedPlan);
// //     }

// //     // ── Step D: Approve and Transition to EXECUTING ──────────────────────────

// //     /**
// //      * Transitions a {@code PENDING} deployment plan to {@code EXECUTING} and
// //      * triggers the asynchronous cloud execution pipeline.
// //      *
// //      * <p>The HTTP response is returned immediately with status 202 Accepted.
// //      * The actual cloud SDK work happens in {@link #executeCloudDeployment(DeploymentPlan, User)}
// //      * on a separate thread managed by Spring's task executor.</p>
// //      *
// //      * @param planPublicId the public ID of the plan to approve
// //      * @return the plan response showing the transition to EXECUTING status
// //      * @throws ResourceNotFoundException if the plan does not exist or is not owned by the user
// //      * @throws IllegalArgumentException  if the plan is not in PENDING status
// //      */
// //     @Transactional
// //     public DeploymentPlanResponse approveAndExecutePlan(String planPublicId) {
// //         User currentUser = userService.resolveCurrentUser();

// //         DeploymentPlan plan = deploymentPlanRepository
// //             .findByPublicIdAndUser(planPublicId, currentUser)
// //             .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

// //         if (plan.getStatus() != DeploymentStatus.PENDING) {
// //             throw new IllegalArgumentException("PLAN_NOT_PENDING");
// //         }

// //         // Transition to EXECUTING and persist before triggering async work
// //         plan.setStatus(DeploymentStatus.EXECUTING);
// //         plan.setExecutionStartedAt(LocalDateTime.now());
// //         DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

// //         log.info("[DeploymentService] Plan '{}' transitioned to EXECUTING. Triggering async execution.",
// //             savedPlan.getPublicId());

// //         // Trigger async cloud execution — returns immediately
// //         executeCloudDeployment(savedPlan, currentUser);

// //         return toResponse(savedPlan);
// //     }

// //     // ── List Plans ───────────────────────────────────────────────────────────

// //     @Transactional(readOnly = true)
// //     public PagedResponse<DeploymentPlanResponse> listPlansForCurrentUser(
// //         int page,
// //         int size,
// //         DeploymentStatus statusFilter
// //     ) {
// //         User currentUser = userService.resolveCurrentUser();
// //         Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

// //         Page<DeploymentPlan> planPage = (statusFilter != null)
// //             ? deploymentPlanRepository.findAllByUserAndStatusOrderByCreatedAtDesc(
// //                 currentUser, statusFilter, pageable)
// //             : deploymentPlanRepository.findAllByUserOrderByCreatedAtDesc(
// //                 currentUser, pageable);

// //         List<DeploymentPlanResponse> content = planPage.getContent()
// //             .stream()
// //             .map(this::toResponse)
// //             .toList();

// //         return PagedResponse.<DeploymentPlanResponse>builder()
// //             .content(content)
// //             .currentPage(planPage.getNumber())
// //             .totalPages(planPage.getTotalPages())
// //             .totalElements(planPage.getTotalElements())
// //             .pageSize(planPage.getSize())
// //             .isLast(planPage.isLast())
// //             .build();
// //     }

// //     // ── Get Single Plan ──────────────────────────────────────────────────────

// //     @Transactional(readOnly = true)
// //     public DeploymentPlanResponse getPlanById(String planPublicId) {
// //         User currentUser = userService.resolveCurrentUser();

// //         DeploymentPlan plan = deploymentPlanRepository
// //             .findByPublicIdAndUser(planPublicId, currentUser)
// //             .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

// //         return toResponse(plan);
// //     }

// //     // ── Phase 5 Execution Stub ───────────────────────────────────────────────

// //     /**
// //      * Asynchronous cloud deployment executor.
// //      *
// //      * <p>This method runs on a separate thread (Spring's task executor) and is
// //      * responsible for the actual cloud SDK calls (AWS SDK v2 / Azure SDK).
// //      * The plan status is updated to {@code SUCCESS} or {@code FAILED} upon
// //      * completion regardless of outcome.</p>
// //      *
// //      * <p><strong>Phase 5 implementation will replace the stub body with:</strong>
// //      * <ol>
// //      *   <li>Decrypt the user's cloud credentials via {@code CredentialVaultService}.</li>
// //      *   <li>Parse {@code plan.getSdkParamsJson()} to determine which AWS/Azure
// //      *       services to provision (EC2, ALB, ASG, RDS, AzureContainerApps, etc.).</li>
// //      *   <li>Build and invoke the appropriate SDK client calls using the decrypted
// //      *       credentials and the parsed parameters.</li>
// //      *   <li>For each SDK call, append a progress entry to {@code plan.getProgressLogJson()}
// //      *       and persist the update so the polling endpoint reflects live progress.</li>
// //      *   <li>On full completion, populate {@code plan.getProvisionedResourcesJson()}
// //      *       with the real resource IDs, ARNs, and endpoints returned by the SDK.</li>
// //      *   <li>Set status to {@code SUCCESS} and {@code completedAt} timestamp.</li>
// //      *   <li>On any SDK exception, set status to {@code FAILED}, log the error
// //      *       detail into {@code progressLogJson}, and write an audit log entry.</li>
// //      * </ol>
// //      * </p>
// //      *
// //      * @param plan        the EXECUTING plan entity (detached from the calling transaction)
// //      * @param executingUser the user who owns the plan and whose credentials will be decrypted
// //      */
// //     @Async
// //     protected void executeCloudDeployment(DeploymentPlan plan, User executingUser) {
// //         // TODO (Phase 5): Replace this stub with full AWS SDK v2 / Azure SDK execution logic.
// //         // The stub immediately marks the plan as SUCCESS for integration testing purposes.
// //         // In Phase 5, this will drive the full provisioning pipeline.

// //         log.info("[DeploymentService] [STUB] Async execution started for plan '{}'. " +
// //                  "Provider: {}. Phase 5 will implement real SDK calls here.",
// //             plan.getPublicId(), plan.getTargetProvider());

// //         try {
// //             // Simulate async work duration for Phase 4 integration testing
// //             Thread.sleep(2000);

// //             DeploymentPlan reloadedPlan = deploymentPlanRepository
// //                 .findByPublicId(plan.getPublicId())
// //                 .orElse(null);

// //             if (reloadedPlan == null) {
// //                 log.error("[DeploymentService] Plan '{}' not found during async execution.",
// //                     plan.getPublicId());
// //                 return;
// //             }

// //             reloadedPlan.setStatus(DeploymentStatus.SUCCESS);
// //             reloadedPlan.setCompletedAt(LocalDateTime.now());
// //             reloadedPlan.setProgressLogJson(
// //                 "[{\"timestamp\":\"" + LocalDateTime.now() + "\"," +
// //                 "\"step\":\"Phase 5 stub: execution completed successfully.\"}]"
// //             );
// //             reloadedPlan.setProvisionedResourcesJson("[]");
// //             deploymentPlanRepository.save(reloadedPlan);

// //             log.info("[DeploymentService] [STUB] Plan '{}' marked SUCCESS. " +
// //                      "Phase 5 will populate real provisioned resource IDs.",
// //                 plan.getPublicId());

// //         } catch (InterruptedException e) {
// //             Thread.currentThread().interrupt();
// //             log.error("[DeploymentService] Async execution thread interrupted for plan '{}'.",
// //                 plan.getPublicId());
// //             markPlanFailed(plan.getPublicId(), "Execution thread was interrupted.");
// //         } catch (Exception e) {
// //             log.error("[DeploymentService] Unexpected error during stub execution for plan '{}': {}",
// //                 plan.getPublicId(), e.getMessage(), e);
// //             markPlanFailed(plan.getPublicId(), "Unexpected error: " + e.getMessage());
// //         }
// //     }

// //     // ── Internal helpers ─────────────────────────────────────────────────────

// //     /**
// //      * Marks a plan as FAILED. Executed within its own transaction so a failure
// //      * in the async thread can always write the terminal state, even if the
// //      * calling async context has no active transaction.
// //      */
// //     @Transactional
// //     protected void markPlanFailed(String planPublicId, String errorDetail) {
// //         deploymentPlanRepository.findByPublicId(planPublicId).ifPresent(plan -> {
// //             plan.setStatus(DeploymentStatus.FAILED);
// //             plan.setCompletedAt(LocalDateTime.now());
// //             plan.setProgressLogJson(
// //                 "[{\"timestamp\":\"" + LocalDateTime.now() + "\"," +
// //                 "\"step\":\"FAILED: " + errorDetail.replace("\"", "'") + "\"}]"
// //             );
// //             deploymentPlanRepository.save(plan);
// //             log.warn("[DeploymentService] Plan '{}' marked FAILED: {}", planPublicId, errorDetail);
// //         });
// //     }

// //     /**
// //      * Extracts the {@code estimatedMonthlyCostUsd} field from the Gemini JSON config.
// //      * Returns {@code null} if the field is absent or not numeric — the plan is still
// //      * saved, and the cost estimate is treated as unavailable rather than blocking the flow.
// //      */
// //     private Double extractCostEstimate(String validatedConfigJson) {
// //         try {
// //             JsonNode root = objectMapper.readTree(validatedConfigJson);
// //             JsonNode costNode = root.get("estimatedMonthlyCostUsd");
// //             if (costNode != null && costNode.isNumber()) {
// //                 return costNode.asDouble();
// //             }
// //         } catch (JsonProcessingException e) {
// //             log.warn("[DeploymentService] Could not extract cost estimate from config JSON: {}",
// //                 e.getMessage());
// //         }
// //         return null;
// //     }

// //     /**
// //      * Persists an {@link AiComplianceLog} entry. Called after every Gemini invocation,
// //      * success or failure. The {@code deploymentPlan} parameter is nullable — if Gemini
// //      * fails before a plan is saved, the log is still persisted with a null FK.
// //      */
// //     private void saveComplianceLog(
// //         DeploymentPlan deploymentPlan,
// //         User user,
// //         String rawUserPrompt,
// //         String sanitizedPrompt,
// //         String rawGeminiResponse,
// //         Integer totalTokens,
// //         Integer promptTokens,
// //         int latencyMs,
// //         CloudProvider provider,
// //         boolean guardrailTriggered,
// //         String policyViolationType
// //     ) {
// //         try {
// //             AiComplianceLog log = AiComplianceLog.builder()
// //                 .deploymentPlan(deploymentPlan)
// //                 .user(user)
// //                 .rawUserPrompt(rawUserPrompt)
// //                 .sanitizedPrompt(sanitizedPrompt)
// //                 .rawGeminiResponse(rawGeminiResponse != null ? rawGeminiResponse : "N/A")
// //                 .geminiTokenCount(totalTokens)
// //                 .promptTokenCount(promptTokens)
// //                 .executionLatencyMs(latencyMs)
// //                 .targetProvider(provider)
// //                 .guardrailTriggered(guardrailTriggered)
// //                 .policyViolationType(policyViolationType)
// //                 .build();

// //             complianceLogRepository.save(log);
// //         } catch (Exception e) {
// //             // Compliance log failure must never propagate to the user-facing response
// //             Slf4jLogger.error("[DeploymentService] Failed to save AI compliance log: {}",
// //                 e.getMessage(), e);
// //         }
// //     }

// //     /**
// //      * Converts a {@link DeploymentPlan} entity to its outbound {@link DeploymentPlanResponse} DTO.
// //      */
// //     private DeploymentPlanResponse toResponse(DeploymentPlan plan) {
// //         return DeploymentPlanResponse.builder()
// //             .planId(plan.getPublicId())
// //             .status(plan.getStatus())
// //             .userPrompt(plan.getPromptSnapshot())
// //             .aiGeneratedConfig(plan.getExecutionSummaryJson())
// //             .costEstimate(extractCostEstimate(
// //                 plan.getExecutionSummaryJson() != null ? plan.getExecutionSummaryJson() : "{}"))
// //             .credentialId(plan.getCredential().getPublicId())
// //             .createdAt(plan.getCreatedAt())
// //             .updatedAt(plan.getUpdatedAt())
// //             .build();
// //     }

// //     private static final String SYSTEM_PROMPT_PREFIX =
// //         "SYSTEM GUARDRAIL PREPENDED — see GeminiClientService for full text. USER PROMPT: ";

// //     // Workaround for Slf4j logger reference inside a non-static context
// //     private static final org.slf4j.Logger Slf4jLogger =
// //         org.slf4j.LoggerFactory.getLogger(DeploymentService.class);
// // }


// package com.ai.project.service;

// import com.ai.project.dto.DeploymentPlanResponse;
// import com.ai.project.dto.GeneratePlanRequest;
// import com.ai.project.dto.PagedResponse;
// import com.ai.project.entity.AiComplianceLog;
// import com.ai.project.entity.CloudCredential;
// import com.ai.project.entity.DeploymentPlan;
// import com.ai.project.entity.User;
// import com.ai.project.entity.enums.CloudProvider;
// import com.ai.project.entity.enums.DeploymentStatus;
// import com.ai.project.exception.ResourceNotFoundException;
// import com.ai.project.repository.AiComplianceLogRepository;
// import com.ai.project.repository.CloudCredentialRepository;
// import com.ai.project.repository.DeploymentPlanRepository;
// import com.ai.project.service.CredentialVaultService.DecryptedAwsCredential;
// import com.ai.project.service.CredentialVaultService.DecryptedAzureCredential;
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;
// import org.springframework.scheduling.annotation.Async;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.List;

// /**
//  * Core deployment state machine service — updated in Phase 5 with full
//  * AWS/Azure execution and teardown logic.
//  *
//  * <p>Complete state machine:
//  * <pre>
//  *   [no entity] ──generatePlan()──► PENDING
//  *   PENDING ──approveAndExecutePlan()──► EXECUTING
//  *   EXECUTING ──[AWS/Azure SDK success]──► SUCCESS
//  *   EXECUTING ──[SDK exception]──► FAILED
//  *   SUCCESS | FAILED ──teardownPlan()──► DESTROYING ──► DESTROYED | DESTROY_FAILED
//  * </pre>
//  * </p>
//  */
// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class DeploymentService {

//     private final DeploymentPlanRepository   deploymentPlanRepository;
//     private final CloudCredentialRepository  credentialRepository;
//     private final AiComplianceLogRepository  complianceLogRepository;
//     private final GeminiClientService        geminiClientService;
//     private final CredentialVaultService     credentialVaultService;
//     private final AwsProvisioningService     awsProvisioningService;
//     private final AzureProvisioningService   azureProvisioningService;
//     private final UserService                userService;
//     private final ObjectMapper               objectMapper;

//     // ── Generate Plan (Steps A / B / C) ─────────────────────────────────────

//     @Transactional
//     public DeploymentPlanResponse generatePlan(GeneratePlanRequest request) {
//         User currentUser = userService.resolveCurrentUser();

//         CloudCredential credential = credentialRepository
//             .findByPublicIdAndUser(request.getCredentialId(), currentUser)
//             .orElseThrow(() -> new ResourceNotFoundException(
//                 "CloudCredential", request.getCredentialId()
//             ));

//         GeminiClientService.GeminiResult geminiResult;
//         try {
//             geminiResult = geminiClientService.generateInfrastructureConfig(request.getPrompt());
//         } catch (IllegalArgumentException | IllegalStateException geminiEx) {
//             saveComplianceLog(
//                 null, currentUser, request.getPrompt(),
//                 "[SYSTEM GUARDRAIL PREPENDED] " + request.getPrompt(),
//                 "FAILED: " + geminiEx.getMessage(),
//                 null, null, 0,
//                 credential.getProvider(), true, geminiEx.getMessage()
//             );
//             throw geminiEx;
//         }

//         Double costEstimate = extractCostEstimate(geminiResult.validatedConfigJson());

//         DeploymentPlan plan = DeploymentPlan.builder()
//             .user(currentUser)
//             .credential(credential)
//             .promptSnapshot(request.getPrompt())
//             .targetProvider(credential.getProvider())
//             .status(DeploymentStatus.PENDING)
//             .sdkParamsJson(geminiResult.validatedConfigJson())
//             .executionSummaryJson(geminiResult.validatedConfigJson())
//             .progressLogJson("[]")
//             .build();

//         DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

//         saveComplianceLog(
//             savedPlan, currentUser, request.getPrompt(),
//             geminiResult.sanitizedPromptSent(),
//             geminiResult.rawGeminiResponseText(),
//             geminiResult.totalTokenCount(),
//             geminiResult.promptTokenCount(),
//             geminiResult.latencyMs(),
//             credential.getProvider(), false, null
//         );

//         log.info("[DeploymentService] Plan '{}' created with status PENDING for user '{}'.",
//             savedPlan.getPublicId(), currentUser.getEmail());

//         return toResponse(savedPlan);
//     }

//     // ── Approve & Execute (Step D) ────────────────────────────────────────────

//     @Transactional
//     public DeploymentPlanResponse approveAndExecutePlan(String planPublicId) {
//         User currentUser = userService.resolveCurrentUser();

//         DeploymentPlan plan = deploymentPlanRepository
//             .findByPublicIdAndUser(planPublicId, currentUser)
//             .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

//         if (plan.getStatus() != DeploymentStatus.PENDING) {
//             throw new IllegalArgumentException("PLAN_NOT_PENDING");
//         }

//         plan.setStatus(DeploymentStatus.EXECUTING);
//         plan.setExecutionStartedAt(LocalDateTime.now());
//         DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

//         log.info("[DeploymentService] Plan '{}' → EXECUTING. Triggering async provisioning.",
//             savedPlan.getPublicId());

//         executeCloudDeployment(savedPlan, currentUser);

//         return toResponse(savedPlan);
//     }

//     // ── Async Cloud Execution (Step E) ────────────────────────────────────────

//     /**
//      * Asynchronous execution pipeline for cloud resource provisioning.
//      *
//      * <p>Execution steps:
//      * <ol>
//      *   <li>Reload the plan inside this async transaction to ensure a fresh
//      *       Hibernate session (the calling transaction has already committed).</li>
//      *   <li>Decrypt credentials scoped to the plan's target provider.</li>
//      *   <li>Parse the Gemini {@code sdkParamsJson} into a {@link JsonNode}.</li>
//      *   <li>Dispatch to the correct provider service via a switch on
//      *       {@link CloudProvider}.</li>
//      *   <li>On success: build the {@code provisionedResourcesJson} structure,
//      *       append a success progress entry, transition to {@code SUCCESS}.</li>
//      *   <li>On any exception: capture only the exception <em>message</em> (not
//      *       the stack trace) into {@code progressLogJson}, transition to {@code FAILED}.</li>
//      * </ol>
//      * </p>
//      *
//      * @param plan          the plan entity that was just saved as EXECUTING
//      * @param executingUser the user who owns the plan and whose credentials will be decrypted
//      */
//     @Async
//     protected void executeCloudDeployment(DeploymentPlan plan, User executingUser) {
//         String planPublicId = plan.getPublicId();
//         log.info("[DeploymentService] Async execution started for plan: {}", planPublicId);

//         DeploymentPlan reloadedPlan = deploymentPlanRepository
//             .findByPublicId(planPublicId)
//             .orElse(null);

//         if (reloadedPlan == null) {
//             log.error("[DeploymentService] Plan '{}' not found in async thread. Aborting.",
//                 planPublicId);
//             return;
//         }

//         try {
//             JsonNode configNode = parseConfigJson(reloadedPlan.getSdkParamsJson());
//             String   resourceId;

//             switch (reloadedPlan.getTargetProvider()) {

//                 case AWS -> {
//                     DecryptedAwsCredential awsCreds = credentialVaultService.decryptAwsCredential(
//                         reloadedPlan.getCredential().getPublicId(), executingUser
//                     );
//                     appendProgressEntry(reloadedPlan,
//                         "Initiating AWS EC2 provisioning via SDK v2...");
//                     resourceId = awsProvisioningService.provisionEc2(configNode, awsCreds);
//                     appendProgressEntry(reloadedPlan,
//                         "AWS EC2 instance provisioned successfully: " + resourceId);
//                 }

//                 case AZURE -> {
//                     DecryptedAzureCredential azureCreds = credentialVaultService.decryptAzureCredential(
//                         reloadedPlan.getCredential().getPublicId(), executingUser
//                     );
//                     appendProgressEntry(reloadedPlan,
//                         "Initiating Azure VM provisioning via Azure SDK...");
//                     resourceId = azureProvisioningService.provisionVm(configNode, azureCreds);
//                     appendProgressEntry(reloadedPlan,
//                         "Azure VM provisioned successfully: " + resourceId);
//                 }

//                 default -> throw new IllegalStateException(
//                     "Unsupported cloud provider: " + reloadedPlan.getTargetProvider()
//                 );
//             }

//             // Build provisionedResources JSON array
//             String provisionedJson = buildProvisionedResourcesJson(
//                 reloadedPlan.getTargetProvider(), resourceId, configNode
//             );

//             reloadedPlan.setProvisionedResourcesJson(provisionedJson);
//             reloadedPlan.setStatus(DeploymentStatus.SUCCESS);
//             reloadedPlan.setCompletedAt(LocalDateTime.now());
//             appendProgressEntry(reloadedPlan, "Deployment completed with status: SUCCESS.");
//             deploymentPlanRepository.save(reloadedPlan);

//             log.info("[DeploymentService] Plan '{}' → SUCCESS. Resource: {}",
//                 planPublicId, resourceId);

//         } catch (Exception e) {
//             // Only the message is logged to progressLog — stack trace stays server-side
//             log.error("[DeploymentService] Execution failed for plan '{}': {}",
//                 planPublicId, e.getMessage(), e);
//             String sanitizedError = sanitizeErrorMessage(e.getMessage());
//             appendProgressEntry(reloadedPlan, "FAILED: " + sanitizedError);
//             reloadedPlan.setStatus(DeploymentStatus.FAILED);
//             reloadedPlan.setCompletedAt(LocalDateTime.now());
//             deploymentPlanRepository.save(reloadedPlan);
//         }
//     }

//     // ── Teardown Plan ─────────────────────────────────────────────────────────

//     /**
//      * Initiates teardown for a completed deployment plan.
//      *
//      * <p>Preconditions:
//      * <ul>
//      *   <li>Plan must belong to the authenticated user.</li>
//      *   <li>Plan status must be {@code SUCCESS} or {@code FAILED} — not
//      *       {@code PENDING}, {@code EXECUTING}, or already {@code DESTROYED}.</li>
//      *   <li>{@code provisionedResourcesJson} must contain at least one resource entry.</li>
//      * </ul>
//      * </p>
//      *
//      * @param planPublicId the public ID of the plan to tear down
//      * @return the updated plan response showing {@code DESTROYING} status
//      */
//     @Transactional
//     public DeploymentPlanResponse teardownPlan(String planPublicId) {
//         User currentUser = userService.resolveCurrentUser();

//         DeploymentPlan plan = deploymentPlanRepository
//             .findByPublicIdAndUser(planPublicId, currentUser)
//             .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

//         validateTeardownPreconditions(plan);

//         plan.setStatus(DeploymentStatus.DESTROYING);
//         plan.setTeardownStartedAt(LocalDateTime.now());
//         DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

//         log.info("[DeploymentService] Plan '{}' → DESTROYING. Triggering async teardown.",
//             planPublicId);

//         executeTeardown(savedPlan, currentUser);

//         return toResponse(savedPlan);
//     }

//     // ── Async Teardown ────────────────────────────────────────────────────────

//     /**
//      * Asynchronous teardown pipeline.
//      *
//      * <p>Extracts the resource ID from {@code provisionedResourcesJson}, decrypts
//      * credentials, and routes to the correct provider teardown method.
//      * Sets status to {@code DESTROYED} on success, {@code DESTROY_FAILED} on error.</p>
//      */
//     @Async
//     protected void executeTeardown(DeploymentPlan plan, User requestingUser) {
//         String planPublicId = plan.getPublicId();
//         log.info("[DeploymentService] Async teardown started for plan: {}", planPublicId);

//         DeploymentPlan reloadedPlan = deploymentPlanRepository
//             .findByPublicId(planPublicId)
//             .orElse(null);

//         if (reloadedPlan == null) {
//             log.error("[DeploymentService] Plan '{}' not found in teardown async thread.", planPublicId);
//             return;
//         }

//         try {
//             String resourceId = extractResourceIdFromProvisionedJson(
//                 reloadedPlan.getProvisionedResourcesJson()
//             );

//             switch (reloadedPlan.getTargetProvider()) {

//                 case AWS -> {
//                     DecryptedAwsCredential awsCreds = credentialVaultService.decryptAwsCredential(
//                         reloadedPlan.getCredential().getPublicId(), requestingUser
//                     );
//                     String region = extractRegionFromProvisionedJson(
//                         reloadedPlan.getProvisionedResourcesJson()
//                     );
//                     appendProgressEntry(reloadedPlan,
//                         "Initiating AWS EC2 termination for instance: " + resourceId);
//                     awsProvisioningService.teardownEc2(resourceId, region, awsCreds);
//                     appendProgressEntry(reloadedPlan,
//                         "AWS EC2 instance terminated successfully: " + resourceId);
//                 }

//                 case AZURE -> {
//                     DecryptedAzureCredential azureCreds = credentialVaultService.decryptAzureCredential(
//                         reloadedPlan.getCredential().getPublicId(), requestingUser
//                     );
//                     appendProgressEntry(reloadedPlan,
//                         "Initiating Azure Resource Group deletion for VM: " + resourceId);
//                     azureProvisioningService.teardownVm(resourceId, azureCreds);
//                     appendProgressEntry(reloadedPlan,
//                         "Azure VM and Resource Group deleted successfully.");
//                 }

//                 default -> throw new IllegalStateException(
//                     "Unsupported cloud provider for teardown: " + reloadedPlan.getTargetProvider()
//                 );
//             }

//             reloadedPlan.setStatus(DeploymentStatus.DESTROYED);
//             reloadedPlan.setDestroyedAt(LocalDateTime.now());
//             appendProgressEntry(reloadedPlan, "Teardown completed with status: DESTROYED.");
//             deploymentPlanRepository.save(reloadedPlan);

//             log.info("[DeploymentService] Plan '{}' → DESTROYED.", planPublicId);

//         } catch (Exception e) {
//             log.error("[DeploymentService] Teardown failed for plan '{}': {}",
//                 planPublicId, e.getMessage(), e);
//             String sanitizedError = sanitizeErrorMessage(e.getMessage());
//             appendProgressEntry(reloadedPlan, "DESTROY_FAILED: " + sanitizedError);
//             reloadedPlan.setStatus(DeploymentStatus.DESTROY_FAILED);
//             reloadedPlan.setCompletedAt(LocalDateTime.now());
//             deploymentPlanRepository.save(reloadedPlan);
//         }
//     }

//     // ── List / Get ────────────────────────────────────────────────────────────

//     @Transactional(readOnly = true)
//     public PagedResponse<DeploymentPlanResponse> listPlansForCurrentUser(
//         int page, int size, DeploymentStatus statusFilter
//     ) {
//         User currentUser = userService.resolveCurrentUser();
//         Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

//         Page<DeploymentPlan> planPage = (statusFilter != null)
//             ? deploymentPlanRepository.findAllByUserAndStatusOrderByCreatedAtDesc(
//                 currentUser, statusFilter, pageable)
//             : deploymentPlanRepository.findAllByUserOrderByCreatedAtDesc(
//                 currentUser, pageable);

//         List<DeploymentPlanResponse> content = planPage.getContent()
//             .stream()
//             .map(this::toResponse)
//             .toList();

//         return PagedResponse.<DeploymentPlanResponse>builder()
//             .content(content)
//             .currentPage(planPage.getNumber())
//             .totalPages(planPage.getTotalPages())
//             .totalElements(planPage.getTotalElements())
//             .pageSize(planPage.getSize())
//             .isLast(planPage.isLast())
//             .build();
//     }

//     @Transactional(readOnly = true)
//     public DeploymentPlanResponse getPlanById(String planPublicId) {
//         User currentUser = userService.resolveCurrentUser();
//         DeploymentPlan plan = deploymentPlanRepository
//             .findByPublicIdAndUser(planPublicId, currentUser)
//             .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));
//         return toResponse(plan);
//     }

//     // ── Internal helpers ──────────────────────────────────────────────────────

//     private void validateTeardownPreconditions(DeploymentPlan plan) {
//         DeploymentStatus status = plan.getStatus();

//         if (status == DeploymentStatus.PENDING || status == DeploymentStatus.EXECUTING) {
//             throw new IllegalArgumentException(
//                 "PLAN_NOT_PENDING — Cannot tear down a plan in " + status + " state. " +
//                 "Wait for it to reach SUCCESS or FAILED first."
//             );
//         }
//         if (status == DeploymentStatus.DESTROYING) {
//             throw new IllegalArgumentException(
//                 "Teardown is already in progress for plan: " + plan.getPublicId()
//             );
//         }
//         if (status == DeploymentStatus.DESTROYED) {
//             throw new IllegalArgumentException(
//                 "Plan " + plan.getPublicId() + " has already been destroyed."
//             );
//         }

//         String provisionedJson = plan.getProvisionedResourcesJson();
//         if (provisionedJson == null || provisionedJson.isBlank() || provisionedJson.equals("[]")) {
//             throw new IllegalArgumentException(
//                 "Plan " + plan.getPublicId() + " has no provisioned resources to tear down."
//             );
//         }
//     }

//     /**
//      * Appends a structured progress log entry to the plan's {@code progressLogJson} array.
//      * The update is applied in-memory — the caller is responsible for persisting the plan.
//      */
//     private void appendProgressEntry(DeploymentPlan plan, String step) {
//         try {
//             ArrayNode logArray;
//             String existing = plan.getProgressLogJson();

//             if (existing == null || existing.isBlank() || existing.equals("[]")) {
//                 logArray = objectMapper.createArrayNode();
//             } else {
//                 logArray = (ArrayNode) objectMapper.readTree(existing);
//             }

//             ObjectNode entry = objectMapper.createObjectNode();
//             entry.put("timestamp", LocalDateTime.now().toString());
//             entry.put("step",      step);
//             logArray.add(entry);

//             plan.setProgressLogJson(objectMapper.writeValueAsString(logArray));
//         } catch (JsonProcessingException e) {
//             log.warn("[DeploymentService] Failed to append progress entry: {}", e.getMessage());
//         }
//     }

//     /**
//      * Builds the {@code provisionedResourcesJson} structure persisted on SUCCESS.
//      * This is the source-of-truth used by the teardown pipeline to locate resources.
//      */
//     private String buildProvisionedResourcesJson(
//         CloudProvider provider,
//         String resourceId,
//         JsonNode configNode
//     ) throws JsonProcessingException {
//         ArrayNode  resourcesArray = objectMapper.createArrayNode();
//         ObjectNode resource       = objectMapper.createObjectNode();

//         switch (provider) {
//             case AWS -> {
//                 resource.put("resourceType", "EC2_INSTANCE");
//                 resource.put("resourceId",   resourceId);
//                 resource.put("region",
//                     configNode != null && configNode.has("region")
//                         ? configNode.get("region").asText("us-east-1")
//                         : "us-east-1"
//                 );
//             }
//             case AZURE -> {
//                 resource.put("resourceType", "AZURE_VM");
//                 resource.put("resourceId",   resourceId);
//                 resource.put("region",
//                     configNode != null && configNode.has("region")
//                         ? configNode.get("region").asText("eastus")
//                         : "eastus"
//                 );
//             }
//         }

//         resource.put("provisionedAt", LocalDateTime.now().toString());
//         resourcesArray.add(resource);
//         return objectMapper.writeValueAsString(resourcesArray);
//     }

//     /**
//      * Extracts the primary resource ID from the first entry in {@code provisionedResourcesJson}.
//      *
//      * @throws IllegalStateException if the JSON is malformed or the resourceId field is absent
//      */
//     private String extractResourceIdFromProvisionedJson(String provisionedJson) {
//         try {
//             JsonNode array = objectMapper.readTree(provisionedJson);
//             if (!array.isArray() || array.isEmpty()) {
//                 throw new IllegalStateException(
//                     "provisionedResourcesJson is empty — no resources to tear down."
//                 );
//             }
//             JsonNode firstResource = array.get(0);
//             JsonNode resourceIdNode = firstResource.get("resourceId");
//             if (resourceIdNode == null || resourceIdNode.isNull()) {
//                 throw new IllegalStateException(
//                     "provisionedResourcesJson[0].resourceId is null or missing."
//                 );
//             }
//             return resourceIdNode.asText();
//         } catch (JsonProcessingException e) {
//             throw new IllegalStateException(
//                 "Failed to parse provisionedResourcesJson: " + e.getMessage()
//             );
//         }
//     }

//     private String extractRegionFromProvisionedJson(String provisionedJson) {
//         try {
//             JsonNode array = objectMapper.readTree(provisionedJson);
//             if (array.isArray() && !array.isEmpty()) {
//                 JsonNode regionNode = array.get(0).get("region");
//                 if (regionNode != null && !regionNode.isNull()) {
//                     return regionNode.asText("us-east-1");
//                 }
//             }
//         } catch (JsonProcessingException e) {
//             log.warn("[DeploymentService] Could not extract region from provisionedJson: {}",
//                 e.getMessage());
//         }
//         return "us-east-1";
//     }

//     private JsonNode parseConfigJson(String json) {
//         if (json == null || json.isBlank()) return objectMapper.createObjectNode();
//         try {
//             return objectMapper.readTree(json);
//         } catch (JsonProcessingException e) {
//             log.warn("[DeploymentService] Could not parse sdkParamsJson — using empty config: {}",
//                 e.getMessage());
//             return objectMapper.createObjectNode();
//         }
//     }

//     private Double extractCostEstimate(String validatedConfigJson) {
//         if (validatedConfigJson == null || validatedConfigJson.isBlank()) return null;
//         try {
//             JsonNode root     = objectMapper.readTree(validatedConfigJson);
//             JsonNode costNode = root.get("estimatedMonthlyCostUsd");
//             if (costNode != null && costNode.isNumber()) return costNode.asDouble();
//         } catch (JsonProcessingException e) {
//             log.warn("[DeploymentService] Could not extract cost estimate: {}", e.getMessage());
//         }
//         return null;
//     }

//     /**
//      * Saves an {@link AiComplianceLog} record. Failures are swallowed with a
//      * warning log — compliance log failure must never bubble up to the user.
//      */
//     private void saveComplianceLog(
//         DeploymentPlan deploymentPlan,
//         User user,
//         String rawUserPrompt,
//         String sanitizedPrompt,
//         String rawGeminiResponse,
//         Integer totalTokens,
//         Integer promptTokens,
//         int latencyMs,
//         CloudProvider provider,
//         boolean guardrailTriggered,
//         String policyViolationType
//     ) {
//         try {
//             AiComplianceLog entry = AiComplianceLog.builder()
//                 .deploymentPlan(deploymentPlan)
//                 .user(user)
//                 .rawUserPrompt(rawUserPrompt)
//                 .sanitizedPrompt(sanitizedPrompt)
//                 .rawGeminiResponse(rawGeminiResponse != null ? rawGeminiResponse : "N/A")
//                 .geminiTokenCount(totalTokens)
//                 .promptTokenCount(promptTokens)
//                 .executionLatencyMs(latencyMs)
//                 .targetProvider(provider)
//                 .guardrailTriggered(guardrailTriggered)
//                 .policyViolationType(policyViolationType)
//                 .build();
//             complianceLogRepository.save(entry);
//         } catch (Exception e) {
//             log.warn("[DeploymentService] Non-fatal: Failed to save AI compliance log: {}",
//                 e.getMessage());
//         }
//     }

//     /**
//      * Removes any AWS/Azure SDK class names, package paths, or internal
//      * method signatures from exception messages before they reach the database
//      * or client-facing log output.
//      */
//     private String sanitizeErrorMessage(String raw) {
//         if (raw == null) return "An unexpected error occurred.";
//         // Truncate at 500 chars to prevent unbounded progressLogJson growth
//         String trimmed = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
//         // Strip anything that looks like a Java class path reference
//         return trimmed.replaceAll("(software\\.amazon\\.[\\w.]+|com\\.azure\\.[\\w.]+):", "SDK error:");
//     }

//     private DeploymentPlanResponse toResponse(DeploymentPlan plan) {
//         return DeploymentPlanResponse.builder()
//             .planId(plan.getPublicId())
//             .status(plan.getStatus())
//             .userPrompt(plan.getPromptSnapshot())
//             .aiGeneratedConfig(plan.getExecutionSummaryJson())
//             .costEstimate(extractCostEstimate(
//                 plan.getExecutionSummaryJson() != null ? plan.getExecutionSummaryJson() : "{}"
//             ))
//             .credentialId(plan.getCredential().getPublicId())
//             .createdAt(plan.getCreatedAt())
//             .updatedAt(plan.getUpdatedAt())
//             .build();
//     }
// }

package com.ai.project.service;

import com.ai.project.dto.DeploymentPlanResponse;
import com.ai.project.dto.GeneratePlanRequest;
import com.ai.project.dto.PagedResponse;
import com.ai.project.entity.AiComplianceLog;
import com.ai.project.entity.CloudCredential;
import com.ai.project.entity.DeploymentPlan;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.DeploymentStatus;
import com.ai.project.exception.ResourceNotFoundException;
import com.ai.project.repository.AiComplianceLogRepository;
import com.ai.project.repository.CloudCredentialRepository;
import com.ai.project.repository.DeploymentPlanRepository;
import com.ai.project.service.CredentialVaultService.DecryptedAwsCredential;
import com.ai.project.service.CredentialVaultService.DecryptedAzureCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core deployment state machine service — updated with full
 * AWS and advanced Azure execution/teardown routing logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentPlanRepository   deploymentPlanRepository;
    private final CloudCredentialRepository  credentialRepository;
    private final AiComplianceLogRepository  complianceLogRepository;
    private final GeminiClientService        geminiClientService;
    private final CredentialVaultService     credentialVaultService;
    private final AwsProvisioningService     awsProvisioningService;
    private final AzureProvisioningService   azureProvisioningService;
    private final UserService                userService;
    private final ObjectMapper               objectMapper;

    // ── Generate Plan (Steps A / B / C) ─────────────────────────────────────

    @Transactional
    public DeploymentPlanResponse generatePlan(GeneratePlanRequest request) {
        User currentUser = userService.resolveCurrentUser();

        CloudCredential credential = credentialRepository
            .findByPublicIdAndUser(request.getCredentialId(), currentUser)
            .orElseThrow(() -> new ResourceNotFoundException(
                "CloudCredential", request.getCredentialId()
            ));

        GeminiClientService.GeminiResult geminiResult;
        try {
            geminiResult = geminiClientService.generateInfrastructureConfig(request.getPrompt());
        } catch (IllegalArgumentException | IllegalStateException geminiEx) {
            saveComplianceLog(
                null, currentUser, request.getPrompt(),
                "[SYSTEM GUARDRAIL PREPENDED] " + request.getPrompt(),
                "FAILED: " + geminiEx.getMessage(),
                null, null, 0,
                credential.getProvider(), true, geminiEx.getMessage()
            );
            throw geminiEx;
        }

        Double costEstimate = extractCostEstimate(geminiResult.validatedConfigJson());

        DeploymentPlan plan = DeploymentPlan.builder()
            .user(currentUser)
            .credential(credential)
            .promptSnapshot(request.getPrompt())
            .targetProvider(credential.getProvider())
            .status(DeploymentStatus.PENDING)
            .sdkParamsJson(geminiResult.validatedConfigJson())
            .executionSummaryJson(geminiResult.validatedConfigJson())
            .progressLogJson("[]")
            .build();

        DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

        saveComplianceLog(
            savedPlan, currentUser, request.getPrompt(),
            geminiResult.sanitizedPromptSent(),
            geminiResult.rawGeminiResponseText(),
            geminiResult.totalTokenCount(),
            geminiResult.promptTokenCount(),
            geminiResult.latencyMs(),
            credential.getProvider(), false, null
        );

        log.info("[DeploymentService] Plan '{}' created with status PENDING for user '{}'.",
            savedPlan.getPublicId(), currentUser.getEmail());

        return toResponse(savedPlan);
    }

    // ── Approve & Execute (Step D) ────────────────────────────────────────────

    @Transactional
    public DeploymentPlanResponse approveAndExecutePlan(String planPublicId) {
        User currentUser = userService.resolveCurrentUser();

        DeploymentPlan plan = deploymentPlanRepository
            .findByPublicIdAndUser(planPublicId, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

        if (plan.getStatus() != DeploymentStatus.PENDING) {
            throw new IllegalArgumentException("PLAN_NOT_PENDING");
        }

        plan.setStatus(DeploymentStatus.EXECUTING);
        plan.setExecutionStartedAt(LocalDateTime.now());
        DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

        log.info("[DeploymentService] Plan '{}' → EXECUTING. Triggering async provisioning.",
            savedPlan.getPublicId());

        executeCloudDeployment(savedPlan, currentUser);

        return toResponse(savedPlan);
    }

    // ── Async Cloud Execution (Step E) ────────────────────────────────────────

    @Async
    protected void executeCloudDeployment(DeploymentPlan plan, User executingUser) {
        String planPublicId = plan.getPublicId();
        log.info("[DeploymentService] Async execution started for plan: {}", planPublicId);

        DeploymentPlan reloadedPlan = deploymentPlanRepository
            .findByPublicId(planPublicId)
            .orElse(null);

        if (reloadedPlan == null) {
            log.error("[DeploymentService] Plan '{}' not found in async thread. Aborting.",
                planPublicId);
            return;
        }

        try {
            JsonNode configNode = parseConfigJson(reloadedPlan.getSdkParamsJson());
            String   resourceId;

            switch (reloadedPlan.getTargetProvider()) {

                case AWS -> {
                    DecryptedAwsCredential awsCreds = credentialVaultService.decryptAwsCredential(
                        reloadedPlan.getCredential().getPublicId(), executingUser
                    );
                    appendProgressEntry(reloadedPlan,
                        "Initiating AWS EC2 provisioning via SDK v2...");
                    resourceId = awsProvisioningService.provisionEc2(configNode, awsCreds);
                    appendProgressEntry(reloadedPlan,
                        "AWS EC2 instance provisioned successfully: " + resourceId);
                    
                    // AWS DB State Updates
                    String provisionedJson = buildProvisionedResourcesJson(
                        reloadedPlan.getTargetProvider(), resourceId, configNode
                    );
                    reloadedPlan.setProvisionedResourcesJson(provisionedJson);
                    reloadedPlan.setStatus(DeploymentStatus.SUCCESS);
                    reloadedPlan.setCompletedAt(LocalDateTime.now());
                    appendProgressEntry(reloadedPlan, "Deployment completed with status: SUCCESS.");
                    deploymentPlanRepository.save(reloadedPlan);
                    log.info("[DeploymentService] Plan '{}' → SUCCESS. Resource: {}", planPublicId, resourceId);
                }

                case AZURE -> {
                    DecryptedAzureCredential azureCreds = credentialVaultService.decryptAzureCredential(
                        reloadedPlan.getCredential().getPublicId(), executingUser
                    );
                    // Handoff to Azure Provisioning Service (handles its own DB updates & async execution)
                    azureProvisioningService.provisionVmAsync(planPublicId, configNode, azureCreds);
                    
                    // Exit immediately so we don't overwrite Azure's DB updates
                    return;
                }

                default -> throw new IllegalStateException(
                    "Unsupported cloud provider: " + reloadedPlan.getTargetProvider()
                );
            }

        } catch (Exception e) {
            log.error("[DeploymentService] Execution failed for plan '{}': {}",
                planPublicId, e.getMessage(), e);
            String sanitizedError = sanitizeErrorMessage(e.getMessage());
            appendProgressEntry(reloadedPlan, "FAILED: " + sanitizedError);
            reloadedPlan.setStatus(DeploymentStatus.FAILED);
            reloadedPlan.setCompletedAt(LocalDateTime.now());
            deploymentPlanRepository.save(reloadedPlan);
        }
    }

    // ── Teardown Plan ─────────────────────────────────────────────────────────

    @Transactional
    public DeploymentPlanResponse teardownPlan(String planPublicId) {
        User currentUser = userService.resolveCurrentUser();

        DeploymentPlan plan = deploymentPlanRepository
            .findByPublicIdAndUser(planPublicId, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));

        validateTeardownPreconditions(plan);

        plan.setStatus(DeploymentStatus.DESTROYING);
        plan.setTeardownStartedAt(LocalDateTime.now());
        DeploymentPlan savedPlan = deploymentPlanRepository.save(plan);

        log.info("[DeploymentService] Plan '{}' → DESTROYING. Triggering async teardown.",
            planPublicId);

        executeTeardown(savedPlan, currentUser);

        return toResponse(savedPlan);
    }

    // ── Async Teardown ────────────────────────────────────────────────────────

    @Async
    protected void executeTeardown(DeploymentPlan plan, User requestingUser) {
        String planPublicId = plan.getPublicId();
        log.info("[DeploymentService] Async teardown started for plan: {}", planPublicId);

        DeploymentPlan reloadedPlan = deploymentPlanRepository
            .findByPublicId(planPublicId)
            .orElse(null);

        if (reloadedPlan == null) {
            log.error("[DeploymentService] Plan '{}' not found in teardown async thread.", planPublicId);
            return;
        }

        try {
            String resourceId = extractResourceIdFromProvisionedJson(
                reloadedPlan.getProvisionedResourcesJson()
            );

            switch (reloadedPlan.getTargetProvider()) {

                case AWS -> {
                    DecryptedAwsCredential awsCreds = credentialVaultService.decryptAwsCredential(
                        reloadedPlan.getCredential().getPublicId(), requestingUser
                    );
                    String region = extractRegionFromProvisionedJson(
                        reloadedPlan.getProvisionedResourcesJson()
                    );
                    appendProgressEntry(reloadedPlan,
                        "Initiating AWS EC2 termination for instance: " + resourceId);
                    awsProvisioningService.teardownEc2(resourceId, region, awsCreds);
                    appendProgressEntry(reloadedPlan,
                        "AWS EC2 instance terminated successfully: " + resourceId);
                    
                    // AWS DB Updates
                    reloadedPlan.setStatus(DeploymentStatus.DESTROYED);
                    reloadedPlan.setDestroyedAt(LocalDateTime.now());
                    appendProgressEntry(reloadedPlan, "Teardown completed with status: DESTROYED.");
                    deploymentPlanRepository.save(reloadedPlan);
                    log.info("[DeploymentService] Plan '{}' → DESTROYED.", planPublicId);
                }

                case AZURE -> {
                    DecryptedAzureCredential azureCreds = credentialVaultService.decryptAzureCredential(
                        reloadedPlan.getCredential().getPublicId(), requestingUser
                    );
                    String nicResourceId = extractNicResourceIdFromProvisionedJson(
                        reloadedPlan.getProvisionedResourcesJson()
                    );
                    
                    // Handoff to Azure Provisioning Service
                    azureProvisioningService.teardownVmAsync(planPublicId, resourceId, nicResourceId, azureCreds);
                    
                    // Exit immediately so we don't overwrite Azure's DB updates
                    return; 
                }

                default -> throw new IllegalStateException(
                    "Unsupported cloud provider for teardown: " + reloadedPlan.getTargetProvider()
                );
            }

        } catch (Exception e) {
            log.error("[DeploymentService] Teardown failed for plan '{}': {}",
                planPublicId, e.getMessage(), e);
            String sanitizedError = sanitizeErrorMessage(e.getMessage());
            appendProgressEntry(reloadedPlan, "DESTROY_FAILED: " + sanitizedError);
            reloadedPlan.setStatus(DeploymentStatus.DESTROY_FAILED);
            reloadedPlan.setCompletedAt(LocalDateTime.now());
            deploymentPlanRepository.save(reloadedPlan);
        }
    }

    // ── List / Get ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<DeploymentPlanResponse> listPlansForCurrentUser(
        int page, int size, DeploymentStatus statusFilter
    ) {
        User currentUser = userService.resolveCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<DeploymentPlan> planPage = (statusFilter != null)
            ? deploymentPlanRepository.findAllByUserAndStatusOrderByCreatedAtDesc(
                currentUser, statusFilter, pageable)
            : deploymentPlanRepository.findAllByUserOrderByCreatedAtDesc(
                currentUser, pageable);

        List<DeploymentPlanResponse> content = planPage.getContent()
            .stream()
            .map(this::toResponse)
            .toList();

        return PagedResponse.<DeploymentPlanResponse>builder()
            .content(content)
            .currentPage(planPage.getNumber())
            .totalPages(planPage.getTotalPages())
            .totalElements(planPage.getTotalElements())
            .pageSize(planPage.getSize())
            .isLast(planPage.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public DeploymentPlanResponse getPlanById(String planPublicId) {
        User currentUser = userService.resolveCurrentUser();
        DeploymentPlan plan = deploymentPlanRepository
            .findByPublicIdAndUser(planPublicId, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("DeploymentPlan", planPublicId));
        return toResponse(plan);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void validateTeardownPreconditions(DeploymentPlan plan) {
        DeploymentStatus status = plan.getStatus();

        if (status == DeploymentStatus.PENDING || status == DeploymentStatus.EXECUTING) {
            throw new IllegalArgumentException(
                "PLAN_NOT_PENDING — Cannot tear down a plan in " + status + " state. " +
                "Wait for it to reach SUCCESS or FAILED first."
            );
        }
        if (status == DeploymentStatus.DESTROYING) {
            throw new IllegalArgumentException(
                "Teardown is already in progress for plan: " + plan.getPublicId()
            );
        }
        if (status == DeploymentStatus.DESTROYED) {
            throw new IllegalArgumentException(
                "Plan " + plan.getPublicId() + " has already been destroyed."
            );
        }

        String provisionedJson = plan.getProvisionedResourcesJson();
        if (provisionedJson == null || provisionedJson.isBlank() || provisionedJson.equals("[]")) {
            throw new IllegalArgumentException(
                "Plan " + plan.getPublicId() + " has no provisioned resources to tear down."
            );
        }
    }

    private void appendProgressEntry(DeploymentPlan plan, String step) {
        try {
            ArrayNode logArray;
            String existing = plan.getProgressLogJson();

            if (existing == null || existing.isBlank() || existing.equals("[]")) {
                logArray = objectMapper.createArrayNode();
            } else {
                logArray = (ArrayNode) objectMapper.readTree(existing);
            }

            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("timestamp", LocalDateTime.now().toString());
            entry.put("step",      step);
            logArray.add(entry);

            plan.setProgressLogJson(objectMapper.writeValueAsString(logArray));
        } catch (JsonProcessingException e) {
            log.warn("[DeploymentService] Failed to append progress entry: {}", e.getMessage());
        }
    }

    private String buildProvisionedResourcesJson(
        CloudProvider provider,
        String resourceId,
        JsonNode configNode
    ) throws JsonProcessingException {
        ArrayNode  resourcesArray = objectMapper.createArrayNode();
        ObjectNode resource       = objectMapper.createObjectNode();

        switch (provider) {
            case AWS -> {
                resource.put("resourceType", "EC2_INSTANCE");
                resource.put("resourceId",   resourceId);
                resource.put("region",
                    configNode != null && configNode.has("region")
                        ? configNode.get("region").asText("us-east-1")
                        : "us-east-1"
                );
            }
            case AZURE -> {
                resource.put("resourceType", "AZURE_VM");
                resource.put("resourceId",   resourceId);
                resource.put("region",
                    configNode != null && configNode.has("region")
                        ? configNode.get("region").asText("eastus")
                        : "eastus"
                );
            }
        }

        resource.put("provisionedAt", LocalDateTime.now().toString());
        resourcesArray.add(resource);
        return objectMapper.writeValueAsString(resourcesArray);
    }

    private String extractResourceIdFromProvisionedJson(String provisionedJson) {
        try {
            JsonNode array = objectMapper.readTree(provisionedJson);
            if (!array.isArray() || array.isEmpty()) {
                throw new IllegalStateException(
                    "provisionedResourcesJson is empty — no resources to tear down."
                );
            }
            JsonNode firstResource = array.get(0);
            JsonNode resourceIdNode = firstResource.get("resourceId");
            if (resourceIdNode == null || resourceIdNode.isNull()) {
                throw new IllegalStateException(
                    "provisionedResourcesJson[0].resourceId is null or missing."
                );
            }
            return resourceIdNode.asText();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Failed to parse provisionedResourcesJson: " + e.getMessage()
            );
        }
    }

    // New helper specifically for Azure Teardown to find the NIC
    private String extractNicResourceIdFromProvisionedJson(String provisionedJson) {
        try {
            JsonNode array = objectMapper.readTree(provisionedJson);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    if ("AZURE_NIC".equals(node.path("resourceType").asText())) {
                        return node.path("resourceId").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DeploymentService] Could not extract NIC resource ID: {}", e.getMessage());
        }
        return null; // Safe to return null, teardown service will skip NIC deletion
    }

    private String extractRegionFromProvisionedJson(String provisionedJson) {
        try {
            JsonNode array = objectMapper.readTree(provisionedJson);
            if (array.isArray() && !array.isEmpty()) {
                JsonNode regionNode = array.get(0).get("region");
                if (regionNode != null && !regionNode.isNull()) {
                    return regionNode.asText("us-east-1");
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("[DeploymentService] Could not extract region from provisionedJson: {}",
                e.getMessage());
        }
        return "us-east-1";
    }

    private JsonNode parseConfigJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("[DeploymentService] Could not parse sdkParamsJson — using empty config: {}",
                e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private Double extractCostEstimate(String validatedConfigJson) {
        if (validatedConfigJson == null || validatedConfigJson.isBlank()) return null;
        try {
            JsonNode root     = objectMapper.readTree(validatedConfigJson);
            JsonNode costNode = root.get("estimatedMonthlyCostUsd");
            if (costNode != null && costNode.isNumber()) return costNode.asDouble();
        } catch (JsonProcessingException e) {
            log.warn("[DeploymentService] Could not extract cost estimate: {}", e.getMessage());
        }
        return null;
    }

    private void saveComplianceLog(
        DeploymentPlan deploymentPlan,
        User user,
        String rawUserPrompt,
        String sanitizedPrompt,
        String rawGeminiResponse,
        Integer totalTokens,
        Integer promptTokens,
        int latencyMs,
        CloudProvider provider,
        boolean guardrailTriggered,
        String policyViolationType
    ) {
        try {
            AiComplianceLog entry = AiComplianceLog.builder()
                .deploymentPlan(deploymentPlan)
                .user(user)
                .rawUserPrompt(rawUserPrompt)
                .sanitizedPrompt(sanitizedPrompt)
                .rawGeminiResponse(rawGeminiResponse != null ? rawGeminiResponse : "N/A")
                .geminiTokenCount(totalTokens)
                .promptTokenCount(promptTokens)
                .executionLatencyMs(latencyMs)
                .targetProvider(provider)
                .guardrailTriggered(guardrailTriggered)
                .policyViolationType(policyViolationType)
                .build();
            complianceLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[DeploymentService] Non-fatal: Failed to save AI compliance log: {}",
                e.getMessage());
        }
    }

    private String sanitizeErrorMessage(String raw) {
        if (raw == null) return "An unexpected error occurred.";
        String trimmed = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
        return trimmed.replaceAll("(software\\.amazon\\.[\\w.]+|com\\.azure\\.[\\w.]+):", "SDK error:");
    }

    private DeploymentPlanResponse toResponse(DeploymentPlan plan) {
        return DeploymentPlanResponse.builder()
            .planId(plan.getPublicId())
            .status(plan.getStatus())
            .userPrompt(plan.getPromptSnapshot())
            .aiGeneratedConfig(plan.getExecutionSummaryJson())
            .costEstimate(extractCostEstimate(
                plan.getExecutionSummaryJson() != null ? plan.getExecutionSummaryJson() : "{}"
            ))
            .credentialId(plan.getCredential().getPublicId())
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .build();
    }
}