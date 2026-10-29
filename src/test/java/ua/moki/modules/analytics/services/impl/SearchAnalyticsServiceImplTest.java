package ua.moki.modules.analytics.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.analytics.domains.SearchLog;
import ua.moki.modules.analytics.dtos.SearchAnalyticsDashboardDTO;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.analytics.repositories.SearchAnalyticsRepository;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchAnalyticsServiceImplTest {

    @Mock
    private SearchAnalyticsRepository searchAnalyticsRepository;

    private Clock clock;
    private SearchAnalyticsServiceImpl searchAnalyticsService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-29T08:00:00Z"), ZoneOffset.UTC);
        searchAnalyticsService = new SearchAnalyticsServiceImpl(searchAnalyticsRepository, clock);
    }

    @Test
    void recordSearchEvent_shouldSaveNormalizedTextQueryAndDeduplicateWithinWindow() {
        UUID userId = UUID.randomUUID();
        SearchPerformedEvent event = new SearchPerformedEvent(
                "  Мигдаль   Золотий ",
                5L,
                ProductCategory.NUTS,
                List.of("Мигдаль"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                false,
                userId,
                "uk",
                "192.168.1.10",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X)",
                OffsetDateTime.now(clock)
        );

        searchAnalyticsService.recordSearchEvent(event);
        // Повторний ідентичний запит від того ж користувача у межах 3 хвилин має ігноруватися
        searchAnalyticsService.recordSearchEvent(event);

        ArgumentCaptor<SearchLog> captor = ArgumentCaptor.forClass(SearchLog.class);
        verify(searchAnalyticsRepository, times(1)).save(captor.capture());

        SearchLog saved = captor.getValue();
        assertThat(saved.getQuery()).isEqualTo("Мигдаль   Золотий");
        assertThat(saved.getNormalizedQuery()).isEqualTo("мигдаль золотий");
        assertThat(saved.getResultsCount()).isEqualTo(5L);
        assertThat(saved.getZeroResults()).isFalse();
        assertThat(saved.getCategory()).isEqualTo(ProductCategory.NUTS);
        assertThat(saved.getSubcategories()).isEqualTo("Мигдаль");
        assertThat(saved.getUserPublicId()).isEqualTo(userId);
    }

    @Test
    void recordSearchEvent_shouldSaveCatalogFilterSearchWithoutTextQuery() {
        SearchPerformedEvent catalogEvent = new SearchPerformedEvent(
                null,
                0L,
                ProductCategory.DRIED_FRUITS,
                List.of("Манго", "Інжир"),
                null,
                null,
                true,
                null,
                "uk",
                "10.0.0.1",
                "Mozilla/5.0",
                OffsetDateTime.now(clock)
        );

        searchAnalyticsService.recordSearchEvent(catalogEvent);

        ArgumentCaptor<SearchLog> captor = ArgumentCaptor.forClass(SearchLog.class);
        verify(searchAnalyticsRepository).save(captor.capture());

        SearchLog saved = captor.getValue();
        assertThat(saved.getQuery()).isNull();
        assertThat(saved.getNormalizedQuery()).isNull();
        assertThat(saved.getCategory()).isEqualTo(ProductCategory.DRIED_FRUITS);
        assertThat(saved.getSubcategories()).isEqualTo("Манго, Інжир");
        assertThat(saved.getHasDiscount()).isTrue();
        assertThat(saved.getZeroResults()).isTrue();
    }

    @Test
    void recordSearchEvent_shouldIgnoreBots() {
        SearchPerformedEvent botEvent = new SearchPerformedEvent(
                "фісташки",
                10L,
                null,
                null,
                null,
                null,
                null,
                null,
                "uk",
                "66.249.66.1",
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                OffsetDateTime.now(clock)
        );

        searchAnalyticsService.recordSearchEvent(botEvent);

        verify(searchAnalyticsRepository, never()).save(any());
    }

    @Test
    void getDashboardStats_shouldCalculateRatesAndMapAggregations() {
        when(searchAnalyticsRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(100L);
        when(searchAnalyticsRepository.countByCreatedAtGreaterThanEqualAndNormalizedQueryIsNotNull(any())).thenReturn(60L);
        when(searchAnalyticsRepository.countByCreatedAtGreaterThanEqualAndZeroResultsTrue(any())).thenReturn(15L);

        when(searchAnalyticsRepository.findTopQueries(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"мигдаль", 25L, 8.4, 2L}));
        when(searchAnalyticsRepository.findTopZeroResultQueries(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"макадамія", 7L}));
        when(searchAnalyticsRepository.findTopCategories(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ProductCategory.NUTS, 40L}));
        when(searchAnalyticsRepository.findDailyStats(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-10-29", 100L, 60L, 15L}));

        SearchAnalyticsDashboardDTO dashboard = searchAnalyticsService.getDashboardStats(30);

        assertThat(dashboard.totalSearches()).isEqualTo(100L);
        assertThat(dashboard.textSearches()).isEqualTo(60L);
        assertThat(dashboard.catalogFilterSearches()).isEqualTo(40L);
        assertThat(dashboard.zeroResultSearches()).isEqualTo(15L);
        assertThat(dashboard.zeroResultRate()).isEqualTo(15.0);
        assertThat(dashboard.topQueries()).hasSize(1);
        assertThat(dashboard.topQueries().getFirst().query()).isEqualTo("мигдаль");
        assertThat(dashboard.topZeroResultQueries()).hasSize(1);
        assertThat(dashboard.topZeroResultQueries().getFirst().query()).isEqualTo("макадамія");
        assertThat(dashboard.topCategories()).hasSize(1);
        assertThat(dashboard.topCategories().getFirst().categoryTitle()).isEqualTo("Горіхи");
    }
}
