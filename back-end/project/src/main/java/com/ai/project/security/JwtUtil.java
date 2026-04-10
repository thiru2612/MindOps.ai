package com.ai.project.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Stateless JWT utility for generating, validating, and parsing both
 * access tokens (15 min) and refresh tokens (7 days).
 *
 * <p>Token design:
 * <ul>
 *   <li>Algorithm: HS512 (HMAC-SHA-512)</li>
 *   <li>Each token carries a {@code jti} (JWT ID) claim — a UUID v4 — used
 *       by the blocklist mechanism to revoke individual tokens without
 *       invalidating the user's entire key.</li>
 *   <li>Access tokens carry {@code sub} (email), {@code userId} (publicId),
 *       {@code role}, and {@code type=ACCESS}.</li>
 *   <li>Refresh tokens carry only {@code sub} (email) and {@code type=REFRESH}.
 *       They deliberately omit role/userId to minimise exposure if intercepted.</li>
 *   <li>Access and refresh tokens are signed with <em>separate</em> secrets,
 *       preventing a refresh token from being submitted as an access token.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID  = "userId";
    private static final String CLAIM_ROLE     = "role";
    private static final String CLAIM_TYPE     = "type";
    private static final String TYPE_ACCESS    = "ACCESS";
    private static final String TYPE_REFRESH   = "REFRESH";

    @Value("${app.security.jwt.secret}")
    private String accessSecretRaw;

    @Value("${app.security.jwt.expiry-ms}")
    private long accessExpiryMs;

    @Value("${app.security.jwt.refresh-secret}")
    private String refreshSecretRaw;

    @Value("${app.security.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    public void init() {
        validateSecret(accessSecretRaw,  "JWT_SECRET");
        validateSecret(refreshSecretRaw, "JWT_REFRESH_SECRET");
        this.accessKey  = Keys.hmacShaKeyFor(accessSecretRaw.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecretRaw.getBytes(StandardCharsets.UTF_8));
        log.info("[JwtUtil] Access and Refresh signing keys initialised (HS512).");
    }

    // ── Token Generation ─────────────────────────────────────────────────────

    /**
     * Generates a short-lived access token (15 min by default).
     *
     * @param email    the user's email (JWT subject)
     * @param publicId the user's public-facing ID (userId claim)
     * @param role     the user's RBAC role (role claim)
     * @return signed compact JWT string
     */
    public String generateAccessToken(String email, String publicId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim(CLAIM_USER_ID, publicId)
            .claim(CLAIM_ROLE,    role)
            .claim(CLAIM_TYPE,    TYPE_ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessExpiryMs)))
            .signWith(accessKey, Jwts.SIG.HS512)
            .compact();
    }

    /**
     * Generates a long-lived refresh token (7 days by default).
     * Intentionally carries minimal claims.
     *
     * @param email the user's email (JWT subject)
     * @return signed compact JWT string
     */
    public String generateRefreshToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(refreshExpiryMs)))
            .signWith(refreshKey, Jwts.SIG.HS512)
            .compact();
    }

    // ── Claim Extraction — Access Tokens ────────────────────────────────────

    public String extractEmailFromAccessToken(String token) {
        return extractClaim(token, accessKey, Claims::getSubject);
    }

    public String extractJtiFromAccessToken(String token) {
        return extractClaim(token, accessKey, Claims::getId);
    }

    public String extractUserIdFromAccessToken(String token) {
        return extractClaim(token, accessKey,
            claims -> claims.get(CLAIM_USER_ID, String.class));
    }

    public String extractRoleFromAccessToken(String token) {
        return extractClaim(token, accessKey,
            claims -> claims.get(CLAIM_ROLE, String.class));
    }

    public Date extractExpirationFromAccessToken(String token) {
        return extractClaim(token, accessKey, Claims::getExpiration);
    }

    // ── Claim Extraction — Refresh Tokens ───────────────────────────────────

    public String extractEmailFromRefreshToken(String token) {
        return extractClaim(token, refreshKey, Claims::getSubject);
    }

    public String extractJtiFromRefreshToken(String token) {
        return extractClaim(token, refreshKey, Claims::getId);
    }

    public Date extractExpirationFromRefreshToken(String token) {
        return extractClaim(token, refreshKey, Claims::getExpiration);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates an access token's signature, expiry, and type claim.
     *
     * @param token the raw JWT string from the Authorization header
     * @return true only if the token is structurally valid, unexpired, and typed ACCESS
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessKey, TYPE_ACCESS);
    }

    /**
     * Validates a refresh token's signature, expiry, and type claim.
     *
     * @param token the raw JWT string from the request body
     * @return true only if the token is structurally valid, unexpired, and typed REFRESH
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshKey, TYPE_REFRESH);
    }

    // ── Expiry Helpers ───────────────────────────────────────────────────────

    /** Returns the configured access token lifetime in milliseconds. */
    public long getAccessExpiryMs() {
        return accessExpiryMs;
    }

    /** Returns the configured refresh token lifetime in milliseconds. */
    public long getRefreshExpiryMs() {
        return refreshExpiryMs;
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private <T> T extractClaim(String token, SecretKey key, Function<Claims, T> claimsResolver) {
        Claims claims = parseAllClaims(token, key);
        return claimsResolver.apply(claims);
    }

    private Claims parseAllClaims(String token, SecretKey key) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean validateToken(String token, SecretKey key, String expectedType) {
        try {
            Claims claims = parseAllClaims(token, key);
            String actualType = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(actualType)) {
                log.warn("[JwtUtil] Token type mismatch. Expected: {}, Got: {}", expectedType, actualType);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JwtUtil] Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JwtUtil] Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JwtUtil] Malformed JWT: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("[JwtUtil] Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JwtUtil] JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private void validateSecret(String secret, String envVarName) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "[JwtUtil] " + envVarName + " environment variable is not set."
            );
        }
        // HS512 requires a minimum key size of 512 bits (64 bytes)
        if (secret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException(
                "[JwtUtil] " + envVarName + " must be at least 64 bytes for HS512. " +
                "Regenerate with: openssl rand -base64 64"
            );
        }
    }
}