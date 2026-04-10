package com.ai.project.security;

import com.ai.project.repository.TokenBlocklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled maintenance job that purges expired JWT entries from the
 * {@code token_blocklist} table, preventing unbounded database growth.
 *
 * <p><strong>Why expired tokens are safe to delete:</strong>
 * A token in the blocklist that is past its {@code expires_at} timestamp
 * would be rejected by {@link JwtUtil#validateAccessToken} on signature
 * expiry validation — independently of the blocklist check. The blocklist
 * entry is therefore redundant once the token has expired, and purging it
 * carries zero security risk.</p>
 *
 * <p><strong>Schedule:</strong> Runs once per day at 03:00 UTC
 * ({@code 0 0 3 * * ?}). This off-peak window minimises contention with
 * the hot-path {@code existsByJti} lookup that runs on every authenticated
 * API request. The cron expression uses Spring's standard six-field format:
 * {@code second minute hour day-of-month month day-of-week}.</p>
 *
 * <p><strong>Bounded table size analysis:</strong>
 * Maximum live rows at any time ≈ {@code active_users × 2 tokens × 15min window}.
 * For 10,000 active users this is at most 20,000 rows — negligible for MySQL
 * with the {@code idx_tbl_jti} index. The daily purge ensures the table never
 * accumulates historical data beyond a 24-hour window.</p>
 *
 * <p><strong>{@code @EnableScheduling}</strong> is declared on
 * {@code MindOpsCloudApplication} — no additional configuration is required
 * for this scheduler to activate.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenBlocklistRepository tokenBlocklistRepository;

    /**
     * Deletes all {@code token_blocklist} rows where {@code expires_at < NOW()}.
     *
     * <p>Runs within a dedicated transaction so the bulk DELETE is atomic.
     * If the delete fails (e.g. a transient DB connection issue), the job
     * logs the error and exits cleanly — it will retry at the next scheduled run.
     * The application continues operating normally regardless of cleanup job status.</p>
     *
     * <p>Cron: {@code 0 0 3 * * ?} — every day at 03:00:00 UTC.</p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("[TokenCleanupScheduler] Starting expired token blocklist purge at: {}",
            LocalDateTime.now());

        int deletedCount = 0;
        try {
            deletedCount = tokenBlocklistRepository.deleteAllExpiredBefore(LocalDateTime.now());
            log.info("[TokenCleanupScheduler] Purge complete. Deleted {} expired token " +
                     "blocklist entries.", deletedCount);
        } catch (Exception e) {
            // Non-fatal — log and exit. The scheduler will retry on next cycle.
            log.error("[TokenCleanupScheduler] Purge job failed with error: {}. " +
                      "Will retry at next scheduled run (03:00 UTC).", e.getMessage(), e);
        }
    }
}