package com.ai.project.entity;
import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.CredentialValidationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "cloud_credentials",
    indexes = {
        @Index(name = "idx_cred_public_id", columnList = "public_id", unique = true),
        @Index(name = "idx_cred_user_id",   columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_credential_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 10)
    private CloudProvider provider;

    @Column(name = "credential_label", nullable = false, length = 100)
    private String credentialLabel;

    // ── AWS Fields (AES-256/GCM encrypted at rest) ───────────────────────────

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AZURE credentials.
     */
    @Column(name = "access_key_id_enc", columnDefinition = "TEXT")
    private String accessKeyIdEncrypted;

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AZURE credentials.
     */
    @Column(name = "secret_access_key_enc", columnDefinition = "TEXT")
    private String secretAccessKeyEncrypted;

    @Column(name = "default_region", length = 30)
    private String defaultRegion;

    // ── Azure Fields (AES-256/GCM encrypted at rest) ─────────────────────────

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AWS credentials.
     */
    @Column(name = "tenant_id_enc", columnDefinition = "TEXT")
    private String tenantIdEncrypted;

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AWS credentials.
     */
    @Column(name = "client_id_enc", columnDefinition = "TEXT")
    private String clientIdEncrypted;

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AWS credentials.
     */
    @Column(name = "client_secret_enc", columnDefinition = "TEXT")
    private String clientSecretEncrypted;

    /**
     * Stores format: "BASE64_IV:BASE64_CIPHERTEXT"
     * Null for AWS credentials.
     */
    @Column(name = "subscription_id_enc", columnDefinition = "TEXT")
    private String subscriptionIdEncrypted;

    // ── Status ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private CredentialValidationStatus validationStatus = CredentialValidationStatus.UNVERIFIED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Lifecycle Hooks ──────────────────────────────────────────────────────

    @PrePersist
    private void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = "cred_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}