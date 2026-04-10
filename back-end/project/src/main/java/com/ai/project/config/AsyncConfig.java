package com.ai.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enterprise Async Configuration.
 * Provisions a dedicated thread pool for long-running cloud deployments
 * (AWS and Azure) so the main Tomcat HTTP threads are never blocked.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core threads ready to handle deployments
        executor.setCorePoolSize(5);
        // Max threads during a massive spike
        executor.setMaxPoolSize(20);
        // Queue size before rejecting tasks
        executor.setQueueCapacity(100);
        // Clean naming for your logs
        executor.setThreadNamePrefix("MindOps-Async-");
        executor.initialize();
        return executor;
    }
}