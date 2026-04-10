package com.ai.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@link com.ai.project.config.RateLimitInterceptor} when an
 * authenticated user exhausts their Bucket4j token allowance for the
 * AI generation endpoint.
 *
 * <p>Carries {@code retryAfterSeconds} so the {@link GlobalExceptionHandler}
 * can populate both the JSON body and the {@code Retry-After} HTTP header,
 * allowing frontend clients to implement automatic back-off.</p>
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}