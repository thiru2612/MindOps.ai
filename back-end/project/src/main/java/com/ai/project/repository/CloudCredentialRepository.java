package com.ai.project.repository;

import com.ai.project.entity.CloudCredential;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.CloudProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CloudCredentialRepository extends JpaRepository<CloudCredential, Long> {

    List<CloudCredential> findAllByUserOrderByCreatedAtDesc(User user);

    Optional<CloudCredential> findByPublicIdAndUser(String publicId, User user);

    Optional<CloudCredential> findByPublicId(String publicId);

    boolean existsByPublicIdAndUser(String publicId, User user);

    List<CloudCredential> findAllByUserAndProvider(User user, CloudProvider provider);

    /**
     * Checks if any DeploymentPlan in EXECUTING or DESTROYING status
     * references this credential — used to block deletion.
     */
    @Query("""
        SELECT COUNT(dp) > 0
        FROM DeploymentPlan dp
        WHERE dp.credential.publicId = :credentialPublicId
          AND dp.status IN (
              com.ai.project.entity.enums.DeploymentStatus.EXECUTING,
              com.ai.project.entity.enums.DeploymentStatus.DESTROYING
          )
        """)
    boolean isCredentialActivelyUsed(@Param("credentialPublicId") String credentialPublicId);
}