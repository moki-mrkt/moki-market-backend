package ua.moki.modules.analytics.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.analytics.domains.SearchLog;
import ua.moki.modules.analytics.dtos.SearchAnalyticsDashboardDTO;
import ua.moki.modules.analytics.dtos.SearchLogResponseDTO;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.analytics.jobs.SearchAnalyticsCleanupJob;
import ua.moki.modules.analytics.repositories.SearchAnalyticsRepository;
import ua.moki.modules.analytics.services.SearchAnalyticsService;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchAnalyticsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SearchAnalyticsService searchAnalyticsService;

    @Autowired
    private SearchAnalyticsRepository searchAnalyticsRepository;

    @Autowired
    private SearchAnalyticsCleanupJob searchAnalyticsCleanupJob;

    @Test
    @DisplayName("recordSearchEvent and getDashboardStats persist and aggregate search logs in PostgreSQL")
    void shouldPersistAndAggregateSearchLogs() {
        searchAnalyticsRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now();

        searchAnalyticsService.recordSearchEvent(new SearchPerformedEvent(
                "Кеш'ю смажений",
                4L,
                ProductCategory.NUTS,
                List.of("Кеш'ю"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(600),
                false,
                UUID.randomUUID(),
                "uk",
                "192.168.0.10",
                "Mozilla/5.0",
                now
        ));

        searchAnalyticsService.recordSearchEvent(new SearchPerformedEvent(
                "Пекан карамелізований",
                0L,
                ProductCategory.NUTS,
                null,
                null,
                null,
                null,
                null,
                "uk",
                "192.168.0.11",
                "Mozilla/5.0",
                now
        ));

        // Запит лише з фільтрами каталогу (без текстового query)
        searchAnalyticsService.recordSearchEvent(new SearchPerformedEvent(
                null,
                8L,
                ProductCategory.DRIED_FRUITS,
                List.of("Манго"),
                null,
                null,
                true,
                null,
                "uk",
                "192.168.0.12",
                "Mozilla/5.0",
                now
        ));

        SearchAnalyticsDashboardDTO dashboard = searchAnalyticsService.getDashboardStats(7);

        assertThat(dashboard.totalSearches()).isEqualTo(3L);
        assertThat(dashboard.textSearches()).isEqualTo(2L);
        assertThat(dashboard.catalogFilterSearches()).isEqualTo(1L);
        assertThat(dashboard.zeroResultSearches()).isEqualTo(1L);
        assertThat(dashboard.topQueries()).isNotEmpty();
        assertThat(dashboard.topZeroResultQueries()).hasSize(1);
        assertThat(dashboard.topZeroResultQueries().getFirst().query()).isEqualTo("пекан карамелізований");

        Page<SearchLogResponseDTO> zeroLogs = searchAnalyticsService.getSearchLogs(
                null,
                ProductCategory.NUTS,
                true,
                true,
                7,
                PageRequest.of(0, 10)
        );
        assertThat(zeroLogs.getTotalElements()).isEqualTo(1L);
        assertThat(zeroLogs.getContent().getFirst().normalizedQuery()).isEqualTo("пекан карамелізований");
    }

    @Test
    @DisplayName("deleteOldSearchLogs removes only logs older than retention period")
    void deleteOldSearchLogs_shouldRemoveOnlyExpiredLogs() {
        searchAnalyticsRepository.deleteAll();

        SearchLog oldLog = new SearchLog();
        oldLog.setQuery("старий запит");
        oldLog.setNormalizedQuery("старий запит");
        oldLog.setResultsCount(1L);
        oldLog.setZeroResults(false);
        oldLog.setCreatedAt(OffsetDateTime.now().minusDays(120));
        searchAnalyticsRepository.save(oldLog);

        SearchLog recentLog = new SearchLog();
        recentLog.setQuery("новий запит");
        recentLog.setNormalizedQuery("новий запит");
        recentLog.setResultsCount(3L);
        recentLog.setZeroResults(false);
        recentLog.setCreatedAt(OffsetDateTime.now().minusDays(5));
        searchAnalyticsRepository.save(recentLog);

        searchAnalyticsCleanupJob.deleteOldSearchLogs();

        List<SearchLog> remaining = searchAnalyticsRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getNormalizedQuery()).isEqualTo("новий запит");
    }
}
