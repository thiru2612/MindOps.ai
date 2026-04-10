package com.ai.project.entity;

import com.ai.project.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",     columnList = "email",     unique = true),
        @Index(name = "idx_users_public_id", columnList = "public_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy    = "user",
        cascade     = CascadeType.ALL,
        orphanRemoval = true,
        fetch       = FetchType.LAZY
    )
    @Builder.Default
    private List<CloudCredential> cloudCredentials = new ArrayList<>();

    @OneToMany(
        mappedBy    = "user",
        cascade     = CascadeType.ALL,
        orphanRemoval = true,
        fetch       = FetchType.LAZY
    )
    @Builder.Default
    private List<DeploymentPlan> deploymentPlans = new ArrayList<>();

    @OneToMany(
        mappedBy    = "user",
        cascade     = CascadeType.ALL,
        orphanRemoval = true,
        fetch       = FetchType.LAZY
    )
    @Builder.Default
    private List<TokenBlocklist> tokenBlocklistEntries = new ArrayList<>();

    // ── Lifecycle Hooks ──────────────────────────────────────────────────────

    @PrePersist
    private void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}