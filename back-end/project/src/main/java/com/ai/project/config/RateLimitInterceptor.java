package com.ai.project.config;

import com.ai.project.exception.RateLimitExceededException;
import com.ai.project.security.UserDetailsImpl;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link HandlerInterceptor} that enforces per-user rate limits on the
 * AI deployment generation endpoint.
 *
 * <p>Execution contract:
 * <ol>
 *   <li>Resolves the authenticated user's {@code publicId} from the Spring
 *       Security context. If no authenticated user is present (should not happen
 *       given {@code SecurityConfig} rules), the request is passed through — the
 *       security filter will reject it with 401 first.</li>
 *   <li>Resolves or creates the user's {@link Bucket} via {@link RateLimitConfig}.</li>
 *   <li>Attempts to consume one token via {@link Bucket#tryConsumeAndReturnRemaining(long)}.
 *       This is an atomic operation — no race condition between check and consume.</li>
 *   <li>If the probe succeeds, the remaining token count is written to the
 *       {@code X-RateLimit-Remaining} response header for client awareness.</li>
 *   <li>If the probe fails (bucket empty), throws {@link RateLimitExceededException}
 *       with the nanosecond-precise wait time converted to seconds. The
 *       {@link com.ai.project.exception.GlobalExceptionHandler} catches this and
 *       writes both the {@code Retry-After} header and the JSON 429 response.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT_HEADER     = "X-RateLimit-Limit";

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest  request,
        @NonNull HttpServletResponse response,
        @NonNull Object              handler
    ) {
        String userId = resolveUserId();

        if (userId == null) {
            // Security filter will handle the 401 — let the request pass through
            return true;
        }

        Bucket bucket = rateLimitConfig.resolveBucket(userId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER,
                String.valueOf(probe.getRemainingTokens()));
            response.setHeader(RATE_LIMIT_LIMIT_HEADER,
                String.valueOf(probe.getRemainingTokens() + 1));
            log.debug("[RateLimitInterceptor] Token consumed for user '{}'. Remaining: {}",
                userId, probe.getRemainingTokens());
            return true;
        }

        long retryAfterSeconds = (probe.getNanosToWaitForRefill() / 1_000_000_000L) + 1L;
        log.warn("[RateLimitInterceptor] Rate limit exhausted for user '{}'. " +
                 "Retry after {} seconds.", userId, retryAfterSeconds);

        throw new RateLimitExceededException(retryAfterSeconds);
    }

    /**
     * Extracts the authenticated user's {@code publicId} from the Spring Security context.
     *
     * @return the user's publicId, or {@code null} if the request is unauthenticated
     */
    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            return null;
        }
        return principal.getUser().getPublicId();
    }
}