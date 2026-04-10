package com.ai.project.entity;

import com.ai.project.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_al_public_id",    columnList = "public_id",    unique = true),
        @Index(name = "idx_al_user_id",      columnList = "user_id"),
        @Index(name = "idx_al_action",       columnList = "action"),
        @Index(name = "idx_al_resource_id",  columnList = "resource_id"),
        @Index(name = "idx_al_created_at",   columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    /**
     * Soft reference — user_id is stored as a plain column, not a FK,
     * so that audit history is preserved even if the user account is deleted.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /**
     * The public_id of the affected resource (e.g. plan_xxx, cred_xxx).
     */
    @Column(name = "resource_id", length = 36)
    private String resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Arbitrary JSON metadata relevant to the action
     * (e.g. {"provider":"AWS","credentialId":"cred_xxx"}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle Hooks ──────────────────────────────────────────────────────

    @PrePersist
    private void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = "aud_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}