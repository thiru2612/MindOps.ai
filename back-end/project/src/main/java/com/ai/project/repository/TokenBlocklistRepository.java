package com.ai.project.repository;

import com.ai.project.entity.TokenBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenBlocklistRepository extends JpaRepository<TokenBlocklist, Long> {

    /**
     * Hot-path lookup — called by JwtAuthenticationFilter on every authenticated request.
     * Backed by unique index on jti column.
     */
    boolean existsByJti(String jti);

    /**
     * Scheduled purge — removes tokens whose expiry has passed.
     * These are already rejected by JWT signature validation,
     * so they pose no security risk and can be safely deleted.
     */
    // @Modifying
    // @Query("DELETE FROM TokenBlocklist t WHERE t.expiresAt < :now")
    // int deleteAllExpiredBefore(@Param("now") LocalDateTime now);

    /**
     * Used when deactivating a user — force-invalidate all their live tokens.
     */
    @Modifying
    @Query("DELETE FROM TokenBlocklist t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // @org.springframework.data.jpa.repository.Modifying
    // @org.springframework.data.jpa.repository.Query("DELETE FROM RevokedToken t WHERE t.expiresAt < :now")
    // int deleteAllExpiredBefore(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM TokenBlocklist t WHERE t.expiresAt < :now")
    int deleteAllExpiredBefore(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}