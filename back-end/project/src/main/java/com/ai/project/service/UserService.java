package com.ai.project.service;

import com.ai.project.dto.CredentialResponse;
import com.ai.project.dto.PagedResponse;
import com.ai.project.dto.UserProfileResponse;
import com.ai.project.entity.CloudCredential;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.exception.ResourceNotFoundException;
import com.ai.project.repository.CloudCredentialRepository;
import com.ai.project.repository.DeploymentPlanRepository;
import com.ai.project.repository.UserRepository;
import com.ai.project.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository           userRepository;
    private final CloudCredentialRepository credentialRepository;
    private final DeploymentPlanRepository  deploymentPlanRepository;

    // ── Get own profile ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User currentUser = resolveCurrentUser();

        List<CloudCredential> credentials =
            credentialRepository.findAllByUserOrderByCreatedAtDesc(currentUser);

        boolean awsConfigured = credentials.stream()
            .anyMatch(c -> c.getProvider() == CloudProvider.AWS);
        boolean azureConfigured = credentials.stream()
            .anyMatch(c -> c.getProvider() == CloudProvider.AZURE);

        return UserProfileResponse.builder()
            .userId(currentUser.getPublicId())
            .fullName(currentUser.getFullName())
            .email(currentUser.getEmail())
            .role(currentUser.getRole())
            .isActive(currentUser.getIsActive())
            .createdAt(currentUser.getCreatedAt())
            .credentialSummary(
                UserProfileResponse.CredentialSummary.builder()
                    .awsConfigured(awsConfigured)
                    .azureConfigured(azureConfigured)
                    .build()
            )
            .build();
    }

    // ── Admin: list all users (paginated) ────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(
            page, size, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> userPage = userRepository.findAll(pageable);

        List<UserProfileResponse> content = userPage.getContent().stream()
            .map(user -> {
                long deploymentCount = deploymentPlanRepository.countByUser(user);
                return UserProfileResponse.builder()
                    .userId(user.getPublicId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .deploymentCount(deploymentCount)
                    .build();
            })
            .toList();

        return PagedResponse.<UserProfileResponse>builder()
            .content(content)
            .currentPage(userPage.getNumber())
            .totalPages(userPage.getTotalPages())
            .totalElements(userPage.getTotalElements())
            .pageSize(userPage.getSize())
            .isLast(userPage.isLast())
            .build();
    }

    // ── Admin: soft-delete a user ────────────────────────────────────────────

    @Transactional
    public void deactivateUser(String publicId) {
        User currentUser = resolveCurrentUser();

        if (currentUser.getPublicId().equals(publicId)) {
            throw new IllegalArgumentException(
                "Administrators cannot deactivate their own account."
            );
        }

        int updated = userRepository.deactivateByPublicId(publicId);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", publicId);
        }

        log.info("[UserService] User deactivated by admin '{}': publicId={}",
            currentUser.getEmail(), publicId);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Resolves the fully-loaded {@link User} entity for the currently
     * authenticated principal from the Spring Security context.
     *
     * <p>The {@link UserDetailsImpl} held in the security context was populated
     * by {@code JwtAuthenticationFilter} — its embedded {@link User} entity is
     * the live Hibernate-managed object from the session that loaded it.
     * For write operations, a fresh load via {@code userRepository.findByEmail}
     * is performed to ensure we have the latest state and a managed entity
     * within the current transaction.</p>
     */
    public User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new IllegalStateException("No authenticated principal found in security context.");
        }
        String email = principal.getUsername();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
}