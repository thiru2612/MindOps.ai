package com.ai.project.entity;
import com.ai.project.entity.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "token_blocklist",
    indexes = {
        @Index(name = "idx_tbl_jti",        columnList = "jti",        unique = true),
        @Index(name = "idx_tbl_user_id",    columnList = "user_id"),
        @Index(name = "idx_tbl_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlocklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT ID claim (UUID v4) — the primary lookup key on every request.
     * This index is the critical hot path for the security filter.
     */
    @Column(name = "jti", nullable = false, unique = true, length = 36, updatable = false)
    private String jti;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 10)
    private TokenType tokenType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "user_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "fk_tbl_user")
    )
    private User user;

    /**
     * Copied from the JWT exp claim.
     * Used by the scheduled purge job to delete rows that are now harmless
     * (an expired token would be rejected by signature validation anyway).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt;
}