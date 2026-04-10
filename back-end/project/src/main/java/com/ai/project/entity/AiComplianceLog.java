package com.ai.project.entity;

import com.ai.project.entity.enums.CloudProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "ai_compliance_logs",
    indexes = {
        @Index(name = "idx_acl_public_id",       columnList = "public_id",         unique = true),
        @Index(name = "idx_acl_user_id",          columnList = "user_id"),
        @Index(name = "idx_acl_deployment_plan",  columnList = "deployment_plan_id"),
        @Index(name = "idx_acl_guardrail",        columnList = "guardrail_triggered"),
        @Index(name = "idx_acl_created_at",       columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiComplianceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    /**
     * Nullable — if the Gemini call fails before a DeploymentPlan row is committed,
     * the compliance entry must still be persisted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name           = "deployment_plan_id",
        nullable       = true,
        foreignKey     = @ForeignKey(name = "fk_acl_deployment_plan")
    )
    private DeploymentPlan deploymentPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_acl_user"))
    private User user;

    /**
     * The original, unmodified input from the user.
     */
    @Column(name = "raw_user_prompt", nullable = false, columnDefinition = "TEXT")
    private String rawUserPrompt;

    /**
     * The full prompt including the system instruction prefix that was
     * actually transmitted to the Gemini API.
     */
    @Column(name = "sanitized_prompt", nullable = false, columnDefinition = "TEXT")
    private String sanitizedPrompt;

    /**
     * The complete raw JSON response body from Gemini.
     * MEDIUMTEXT to accommodate large SDK parameter blocks (> 65KB).
     */
    @Column(name = "raw_gemini_response", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String rawGeminiResponse;

    /**
     * Set to TRUE when Gemini returned a FORBIDDEN_OPERATION response
     * or the system prompt guardrail was triggered.
     */
    @Column(name = "guardrail_triggered", nullable = false)
    @Builder.Default
    private Boolean guardrailTriggered = false;

    /**
     * Populated when guardrail_triggered = true.
     * e.g. "DELETION_ATTEMPT", "SCHEMA_MISMATCH", "UNPARSEABLE_RESPONSE"
     */
    @Column(name = "policy_violation_type", length = 100)
    private String policyViolationType;

    /**
     * Total tokens consumed (input + output) as reported by Gemini usage metadata.
     */
    @Column(name = "gemini_token_count")
    private Integer geminiTokenCount;

    /**
     * Input/prompt tokens only (for cost breakdown analysis).
     */
    @Column(name = "prompt_token_count")
    private Integer promptTokenCount;

    /**
     * Wall-clock latency of the Gemini API HTTP round-trip in milliseconds.
     */
    @Column(name = "execution_latency_ms", nullable = false)
    private Integer executionLatencyMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_provider", nullable = false, length = 10)
    private CloudProvider targetProvider;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle Hooks ──────────────────────────────────────────────────────

    @PrePersist
    private void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = "acl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}