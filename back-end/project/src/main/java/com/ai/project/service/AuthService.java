package com.ai.project.service;

import com.ai.project.dto.*;
import com.ai.project.entity.TokenBlocklist;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.Role;
import com.ai.project.entity.enums.TokenType;
import com.ai.project.repository.TokenBlocklistRepository;
import com.ai.project.repository.UserRepository;
import com.ai.project.security.JwtUtil;
import com.ai.project.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Core authentication service handling registration, login, token refresh,
 * logout, and password changes.
 *
 * <p>Transaction strategy:
 * <ul>
 *   <li>Write operations ({@code register}, {@code logout}, {@code changePassword})
 *       are wrapped in {@code @Transactional} to ensure atomicity.</li>
 *   <li>Read-only operations ({@code login}) use the {@link AuthenticationManager}
 *       which handles its own transaction context.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository          userRepository;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final PasswordEncoder         passwordEncoder;
    private final AuthenticationManager   authenticationManager;
    private final JwtUtil                 jwtUtil;

    // ── Register ─────────────────────────────────────────────────────────────

    /**
     * Registers a new user account with {@code ROLE_USER}.
     *
     * @throws IllegalStateException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new IllegalStateException("EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
            .fullName(request.getFullName().trim())
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(Role.ROLE_USER)
            .isActive(true)
            .build();

        User savedUser = userRepository.save(user);
        log.info("[AuthService] New user registered: {} ({})", savedUser.getEmail(), savedUser.getPublicId());

        return AuthResponse.builder()
            .userId(savedUser.getPublicId())
            .email(savedUser.getEmail())
            .fullName(savedUser.getFullName())
            .role(savedUser.getRole().name())
            .createdAt(savedUser.getCreatedAt())
            .build();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user and issues an access + refresh token pair.
     *
     * <p>Delegates credential validation to Spring Security's {@link AuthenticationManager},
     * which internally uses {@code UserDetailsServiceImpl} and {@code BCryptPasswordEncoder}.
     * This centralises authentication logic and ensures consistent error handling
     * (e.g. {@link BadCredentialsException}, {@link DisabledException}).</p>
     *
     * @throws BadCredentialsException if email/password do not match
     * @throws DisabledException       if the account has been deactivated
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().toLowerCase().trim(),
                request.getPassword()
            )
        );

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken  = jwtUtil.generateAccessToken(
            user.getEmail(), user.getPublicId(), user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("[AuthService] Login successful for: {}", user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .accessTokenExpiresIn(jwtUtil.getAccessExpiryMs())
            .refreshTokenExpiresIn(jwtUtil.getRefreshExpiryMs())
            .userId(user.getPublicId())
            .role(user.getRole().name())
            .build();
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    /**
     * Exchanges a valid, non-revoked refresh token for a new access token.
     *
     * <p>Does NOT issue a new refresh token (no refresh token rotation in v1).
     * The existing refresh token remains valid until its natural expiry or explicit logout.</p>
     *
     * @throws IllegalArgumentException if the refresh token is invalid or revoked
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("REFRESH_TOKEN_INVALID");
        }

        String jti = jwtUtil.extractJtiFromRefreshToken(refreshToken);
        if (tokenBlocklistRepository.existsByJti(jti)) {
            throw new IllegalArgumentException("REFRESH_TOKEN_REVOKED");
        }

        String email = jwtUtil.extractEmailFromRefreshToken(refreshToken);
        User user = userRepository.findByEmail(email)
            .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
            .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND_OR_INACTIVE"));

        String newAccessToken = jwtUtil.generateAccessToken(
            user.getEmail(), user.getPublicId(), user.getRole().name()
        );

        log.info("[AuthService] Access token refreshed for: {}", email);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .tokenType("Bearer")
            .accessTokenExpiresIn(jwtUtil.getAccessExpiryMs())
            .build();
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    /**
     * Revokes both the current access token and the supplied refresh token
     * by inserting their JTIs into the {@code token_blocklist} table.
     *
     * <p>After this call:
     * <ul>
     *   <li>The access token's JTI will be rejected by {@link com.ai.project.security.JwtAuthenticationFilter}
     *       on all subsequent requests within its remaining 15-minute window.</li>
     *   <li>The refresh token's JTI will be rejected by {@link #refresh(RefreshRequest)}.</li>
     * </ul>
     * </p>
     *
     * @param currentAccessToken the raw JWT from the {@code Authorization} header (without "Bearer " prefix)
     * @param request            the logout request body containing the refresh token
     */
    @Transactional
    public AuthResponse logout(String currentAccessToken, RefreshRequest request) {
        UserDetailsImpl principal = getAuthenticatedPrincipal();
        User user = principal.getUser();

        // Blocklist the current access token
        String accessJti     = jwtUtil.extractJtiFromAccessToken(currentAccessToken);
        Date   accessExpiry  = jwtUtil.extractExpirationFromAccessToken(currentAccessToken);
        blocklistToken(accessJti, TokenType.ACCESS, accessExpiry, user);

        // Blocklist the refresh token if provided and valid
        String refreshToken = request.getRefreshToken();
        if (refreshToken != null && !refreshToken.isBlank() && jwtUtil.validateRefreshToken(refreshToken)) {
            String refreshJti    = jwtUtil.extractJtiFromRefreshToken(refreshToken);
            Date   refreshExpiry = jwtUtil.extractExpirationFromRefreshToken(refreshToken);

            if (!tokenBlocklistRepository.existsByJti(refreshJti)) {
                blocklistToken(refreshJti, TokenType.REFRESH, refreshExpiry, user);
            }
        }

        log.info("[AuthService] Session terminated for: {}", user.getEmail());

        return AuthResponse.builder()
            .message("Session terminated successfully.")
            .build();
    }

    // ── Password Change ──────────────────────────────────────────────────────

    /**
     * Changes the authenticated user's password after validating their current credentials.
     *
     * @throws BadCredentialsException if {@code currentPassword} does not match the stored hash
     */
    @Transactional
    public AuthResponse changePassword(PasswordChangeRequest request) {
        UserDetailsImpl principal = getAuthenticatedPrincipal();
        User user = principal.getUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("CURRENT_PASSWORD_INCORRECT");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("NEW_PASSWORD_SAME_AS_CURRENT");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("[AuthService] Password changed for: {}", user.getEmail());

        return AuthResponse.builder()
            .message("Password updated successfully.")
            .build();
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    /**
     * Inserts a JTI record into the token blocklist with its expiry timestamp.
     * The expiry is copied from the token's own {@code exp} claim so the scheduled
     * purge job can safely remove it once it is past its natural lifetime.
     */
    private void blocklistToken(String jti, TokenType tokenType, Date expiry, User user) {
        LocalDateTime expiresAt = Instant.ofEpochMilli(expiry.getTime())
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime();

        TokenBlocklist entry = TokenBlocklist.builder()
            .jti(jti)
            .tokenType(tokenType)
            .user(user)
            .expiresAt(expiresAt)
            .build();

        tokenBlocklistRepository.save(entry);
        log.debug("[AuthService] Blocklisted {} token JTI: {}", tokenType, jti);
    }

    /**
     * Retrieves the {@link UserDetailsImpl} of the currently authenticated principal
     * from the Spring Security context.
     *
     * @throws IllegalStateException if called outside of an authenticated request context
     */
    private UserDetailsImpl getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new IllegalStateException("No authenticated principal found in security context.");
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}