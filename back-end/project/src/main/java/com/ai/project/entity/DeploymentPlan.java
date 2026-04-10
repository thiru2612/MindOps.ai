package com.ai.project.entity;

import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.DeploymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "deployment_plans",
    indexes = {
        @Index(name = "idx_dp_public_id",  columnList = "public_id",  unique = true),
        @Index(name = "idx_dp_user_id",    columnList = "user_id"),
        @Index(name = "idx_dp_status",     columnList = "status"),
        @Index(name = "idx_dp_provider",   columnList = "target_provider")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dp_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dp_credential"))
    private CloudCredential credential;

    @Column(name = "prompt_snapshot", nullable = false, columnDefinition = "TEXT")
    private String promptSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_provider", nullable = false, length = 10)
    private CloudProvider targetProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeploymentStatus status = DeploymentStatus.PENDING;

    /**
     * Raw structured SDK parameters returned by Gemini.
     * Used exclusively by the execution service — never serialized to the frontend.
     * Stored as JSON column (MySQL 8 native JSON type via Hibernate).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sdk_params_json", columnDefinition = "JSON")
    private String sdkParamsJson;

    /**
     * The FinOps execution summary returned to the frontend for user review.
     * Stored as JSON column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_summary_json", columnDefinition = "JSON")
    private String executionSummaryJson;

    /**
     * Append-only progress log entries written during execution.
     * Stored as JSON array: [{"timestamp":"...","step":"..."}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress_log_json", columnDefinition = "JSON")
    @Builder.Default
    private String progressLogJson = "[]";

    /**
     * Resources provisioned on SUCCESS.
     * Used by teardown service to drive reverse-order deletion.
     * JSON array: [{"resourceType":"VPC","resourceId":"vpc-xxx","region":"..."}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provisioned_resources_json", columnDefinition = "JSON")
    private String provisionedResourcesJson;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "teardown_reason", columnDefinition = "TEXT")
    private String teardownReason;

    @Column(name = "execution_started_at")
    private LocalDateTime executionStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "teardown_started_at")
    private LocalDateTime teardownStartedAt;

    @Column(name = "destroyed_at")
    private LocalDateTime destroyedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy      = "deploymentPlan",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<AiComplianceLog> complianceLogs = new ArrayList<>();

    // ── Lifecycle Hooks ──────────────────────────────────────────────────────

    @PrePersist
    private void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = "plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}