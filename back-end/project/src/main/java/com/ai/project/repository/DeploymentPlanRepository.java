package com.ai.project.repository;

import com.ai.project.entity.DeploymentPlan;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeploymentPlanRepository extends JpaRepository<DeploymentPlan, Long> {

    Optional<DeploymentPlan> findByPublicId(String publicId);

    Optional<DeploymentPlan> findByPublicIdAndUser(String publicId, User user);

    Page<DeploymentPlan> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<DeploymentPlan> findAllByUserAndStatusOrderByCreatedAtDesc(
        User user, DeploymentStatus status, Pageable pageable
    );

    Page<DeploymentPlan> findAllByUserAndTargetProviderOrderByCreatedAtDesc(
        User user, CloudProvider provider, Pageable pageable
    );

    Page<DeploymentPlan> findAllByUserAndStatusAndTargetProviderOrderByCreatedAtDesc(
        User user, DeploymentStatus status, CloudProvider provider, Pageable pageable
    );

    // ── Admin queries (cross-user) ────────────────────────────────────────────

    Page<DeploymentPlan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DeploymentPlan> findAllByStatusOrderByCreatedAtDesc(
        DeploymentStatus status, Pageable pageable
    );

    @Query("""
        SELECT dp FROM DeploymentPlan dp
        WHERE dp.user.publicId = :userPublicId
        ORDER BY dp.createdAt DESC
        """)
    Page<DeploymentPlan> findAllByUserPublicId(
        @Param("userPublicId") String userPublicId, Pageable pageable
    );

    @Query("""
        SELECT COUNT(dp) FROM DeploymentPlan dp
        WHERE dp.user = :user
        """)
    long countByUser(@Param("user") User user);
}