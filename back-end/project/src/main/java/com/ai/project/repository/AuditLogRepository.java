package com.ai.project.repository;

import com.ai.project.entity.AuditLog;
import com.ai.project.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findAllByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    @Query("""
        SELECT al FROM AuditLog al
        WHERE (:userId   IS NULL OR al.userId = :userId)
          AND (:action   IS NULL OR al.action = :action)
          AND al.createdAt BETWEEN :from AND :to
        ORDER BY al.createdAt DESC
        """)
    Page<AuditLog> findAllFiltered(
        @Param("userId") Long userId,
        @Param("action") AuditAction action,
        @Param("from")   LocalDateTime from,
        @Param("to")     LocalDateTime to,
        Pageable pageable
    );
}