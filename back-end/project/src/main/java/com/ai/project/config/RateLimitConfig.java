package com.ai.project.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bucket4j rate limiting configuration.
 *
 * <p>Architecture decisions:
 * <ul>
 *   <li>Each authenticated user gets their own {@link Bucket} instance, keyed
 *       by their {@code publicId} in a {@link ConcurrentHashMap}. This provides
 *       per-user isolation — one abusive user cannot consume another's quota.</li>
 *   <li>Refill strategy is {@link Refill#intervally(long, Duration)} (greedy
 *       interval refill): all tokens are restored at once after the full hour
 *       window, not drip-fed per minute. This matches the product requirement of
 *       "10 requests per hour" as a hard burst window, not a sliding rate.</li>
 *   <li>In-memory storage is appropriate for Phase 1 single-instance deployments.
 *       For multi-instance/Kubernetes deployments, migrate to Bucket4j's Redis
 *       ProxyManager backend without changing the interceptor contract.</li>
 * </ul>
 * </p>
 */
@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.deployment-generate.requests-per-hour:10}")
    private int requestsPerHour;

    /**
     * Thread-safe per-user bucket store.
     * Key: user publicId (e.g. "usr_8f3a1b2c")
     * Value: the user's Bucket4j token bucket
     */
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    /**
     * Returns the existing bucket for the given user, or creates a new one
     * with the configured capacity if none exists.
     *
     * <p>{@link ConcurrentHashMap#computeIfAbsent} guarantees that the bucket
     * factory lambda executes at most once per key under concurrent access,
     * preventing duplicate bucket creation without requiring explicit locking.</p>
     *
     * @param userId the authenticated user's publicId — used as the bucket key
     * @return the user's token bucket (never null)
     */
    public Bucket resolveBucket(String userId) {
        return userBuckets.computeIfAbsent(userId, this::createNewBucket);
    }

    /**
     * Returns the number of seconds until the next token is available in the
     * given bucket. Used to populate the {@code Retry-After} header and the
     * JSON error response body.
     *
     * @param bucket the user's exhausted bucket
     * @return seconds to wait before the next request will be accepted
     */
    public long getRetryAfterSeconds(Bucket bucket) {
        return bucket.getAvailableTokens() == 0
            ? Duration.ofHours(1).toSeconds()
            : 0L;
    }

    private Bucket createNewBucket(String userId) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerHour,
            Refill.intervally(requestsPerHour, Duration.ofHours(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}