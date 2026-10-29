package ua.moki.modules.analytics.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.analytics.repositories.SearchAnalyticsRepository;

import java.time.Clock;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAnalyticsCleanupJob {

    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final Clock clock;

    @Value("${analytics.search.retention-days:365}")
    private int retentionDays;

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void deleteOldSearchLogs() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(retentionDays);
        int deleted = searchAnalyticsRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up {} search analytics logs older than {} days", deleted, retentionDays);
    }
}
