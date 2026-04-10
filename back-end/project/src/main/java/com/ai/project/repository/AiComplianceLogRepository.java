package com.ai.project.repository;

import com.ai.project.entity.AiComplianceLog;
import com.ai.project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiComplianceLogRepository extends JpaRepository<AiComplianceLog, Long> {

    List<AiComplianceLog> findAllByDeploymentPlanPublicId(String deploymentPlanPublicId);

    Page<AiComplianceLog> findAllByUser(User user, Pageable pageable);

    Page<AiComplianceLog> findAllByGuardrailTriggeredTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        SELECT acl FROM AiComplianceLog acl
        WHERE acl.createdAt BETWEEN :from AND :to
        ORDER BY acl.createdAt DESC
        """)
    Page<AiComplianceLog> findAllBetween(
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to,
        Pageable pageable
    );

    @Query("""
        SELECT SUM(acl.geminiTokenCount) FROM AiComplianceLog acl
        WHERE acl.user = :user
        """)
    Long sumTokensByUser(@Param("user") User user);
}