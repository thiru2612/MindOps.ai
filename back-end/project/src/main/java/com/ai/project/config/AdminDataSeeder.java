package com.ai.project.config;

import com.ai.project.entity.User;
import com.ai.project.entity.enums.Role;
import com.ai.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Idempotent bootstrap seeder for the root admin account.
 *
 * <p>Runs automatically on every application startup via {@link CommandLineRunner}.
 * The check {@code userRepository.existsByRole(ROLE_ADMIN)} ensures this seeder
 * is a no-op once any admin account exists — it will not create duplicates.</p>
 *
 * <p>Security requirements:
 * <ul>
 *   <li>Admin credentials are read exclusively from environment variables
 *       ({@code MINDOPS_ADMIN_EMAIL}, {@code MINDOPS_ADMIN_PASSWORD}).
 *       They are never hardcoded in source or config files.</li>
 *   <li>The password is BCrypt-hashed before persistence — the plaintext
 *       value is held in memory only for the duration of this method.</li>
 *   <li>A startup warning is logged to prompt immediate password rotation
 *       via the PATCH /api/v1/auth/password endpoint after first login.</li>
 *   <li>If either environment variable is missing or blank, the application
 *       fails fast with a descriptive error rather than seeding an insecure
 *       or anonymous admin account.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataSeeder implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        validateAdminConfig();

        if (userRepository.existsByRole(Role.ROLE_ADMIN)) {
            log.info("[AdminDataSeeder] Admin account already exists — skipping seed.");
            return;
        }

        String normalizedEmail = adminEmail.toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn(
                "[AdminDataSeeder] A user with email '{}' already exists but has ROLE_USER. " +
                "Skipping admin seed to avoid conflict. " +
                "Manually update the role to ROLE_ADMIN if intended.",
                normalizedEmail
            );
            return;
        }

        User adminUser = User.builder()
            .fullName("MindOps Platform Administrator")
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(adminPassword))
            .role(Role.ROLE_ADMIN)
            .isActive(true)
            .build();

        User savedAdmin = userRepository.save(adminUser);

        // Explicitly null out the in-memory password reference after use
        adminPassword = null;

        log.warn(
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║              MINDOPS ADMIN ACCOUNT SEEDED                   ║\n" +
            "║  Public ID : {}                              ║\n" +
            "║  Email     : {}                    ║\n" +
            "║  CRITICAL  : Change this password immediately after login.  ║\n" +
            "║  Endpoint  : PATCH /api/v1/auth/password                    ║\n" +
            "╚══════════════════════════════════════════════════════════════╝",
            savedAdmin.getPublicId(),
            savedAdmin.getEmail()
        );
    }

    /**
     * Validates that both admin credentials are present in the environment.
     * Fails fast on startup if either is missing, preventing an insecure
     * or partial admin account from being created.
     */
    private void validateAdminConfig() {
        if (!StringUtils.hasText(adminEmail)) {
            throw new IllegalStateException(
                "[AdminDataSeeder] MINDOPS_ADMIN_EMAIL environment variable is not set. " +
                "Set it before starting the application."
            );
        }
        if (!StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException(
                "[AdminDataSeeder] MINDOPS_ADMIN_PASSWORD environment variable is not set. " +
                "Set it before starting the application."
            );
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException(
                "[AdminDataSeeder] MINDOPS_ADMIN_PASSWORD must be at least 12 characters for the root admin account."
            );
        }
    }
}