package com.ai.project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link RateLimitInterceptor} to apply exclusively to the
 * AI deployment generation endpoint.
 *
 * <p>The interceptor is scoped to {@code /api/v1/deployments/generate} only —
 * not applied globally — to avoid polluting other endpoints with rate limit
 * header noise and bucket lookups.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/v1/deployments/generate");
    }
}