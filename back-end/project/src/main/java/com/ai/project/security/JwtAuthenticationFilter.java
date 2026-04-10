package com.ai.project.security;

import com.ai.project.repository.TokenBlocklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — executes exactly once per HTTP request.
 *
 * <p>Processing pipeline per request:
 * <ol>
 *   <li>Extract the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validate token signature, expiry, and type claim via {@link JwtUtil}.</li>
 *   <li><strong>Blocklist check (security-critical):</strong> Query
 *       {@link TokenBlocklistRepository} by JTI. If the JTI is found, the
 *       token has been explicitly revoked (e.g. via logout) — reject with
 *       {@code 401 Unauthorized} immediately, before setting the security context.</li>
 *   <li>Load {@link UserDetails} from the database to verify the account is
 *       still active and correctly locked/enabled.</li>
 *   <li>Populate the Spring Security context with a fully authenticated token.</li>
 * </ol>
 * </p>
 *
 * <p>The blocklist check deliberately uses {@link TokenBlocklistRepository} directly
 * (not a service layer) to avoid circular bean dependencies between
 * {@code SecurityConfig → AuthService → JwtAuthenticationFilter}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil                  jwtUtil;
    private final UserDetailsServiceImpl   userDetailsService;
    private final TokenBlocklistRepository tokenBlocklistRepository;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest  request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // ── 1. No token present — pass through unauthenticated ───────────────
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // ── 2. Validate token structure, signature, expiry, and type ─────────
        if (!jwtUtil.validateAccessToken(token)) {
            log.debug("[JwtFilter] Invalid or expired access token on request to: {}", request.getRequestURI());
            sendUnauthorizedError(response, "TOKEN_INVALID", "Access token is invalid or has expired.");
            return;
        }

        // ── 3. Extract JTI and check blocklist (revocation check) ─────────────
        final String jti = jwtUtil.extractJtiFromAccessToken(token);

        if (tokenBlocklistRepository.existsByJti(jti)) {
            log.warn("[JwtFilter] Blocked JTI detected: {}. Token has been explicitly revoked.", jti);
            sendUnauthorizedError(response, "TOKEN_REVOKED", "This session has been terminated. Please log in again.");
            return;
        }

        // ── 4. Extract principal and load UserDetails ─────────────────────────
        final String email = jwtUtil.extractEmailFromAccessToken(token);

        if (!StringUtils.hasText(email) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (Exception e) {
            log.warn("[JwtFilter] Could not load user for email '{}': {}", email, e.getMessage());
            sendUnauthorizedError(response, "USER_NOT_FOUND", "Associated user account not found.");
            return;
        }

        // ── 5. Check account is still active and not locked ───────────────────
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            log.warn("[JwtFilter] Account for '{}' is disabled or locked.", email);
            sendUnauthorizedError(response, "ACCOUNT_DISABLED", "This account has been deactivated.");
            return;
        }

        // ── 6. Set authenticated principal in security context ────────────────
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[JwtFilter] Authenticated request for '{}' to: {}", email, request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a structured JSON {@code 401 Unauthorized} response and terminates the filter chain.
     * This prevents the default Spring Security redirect behaviour (which expects a login form)
     * from interfering with our stateless REST API.
     */
    private void sendUnauthorizedError(
        HttpServletResponse response,
        String errorCode,
        String message
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"error\":\"%s\",\"message\":\"%s\"}",
            errorCode,
            message
        ));
    }
}