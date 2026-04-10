package com.ai.project.repository;

import com.ai.project.entity.User;
import com.ai.project.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(String publicId);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.publicId = :publicId")
    int deactivateByPublicId(@Param("publicId") String publicId);

    @Query("SELECT u FROM User u WHERE u.publicId = :publicId AND u.isActive = true")
    Optional<User> findActiveByPublicId(@Param("publicId") String publicId);
}