package ua.moki.modules.analytics.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.analytics.services.SearchAnalyticsService;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchAnalyticsEventListenerTest {

    @Mock
    private SearchAnalyticsService searchAnalyticsService;

    @InjectMocks
    private SearchAnalyticsEventListener searchAnalyticsEventListener;

    @Test
    @DisplayName("handleSearchPerformed delegates event to SearchAnalyticsService")
    void handleSearchPerformed_shouldDelegateToService() {
        SearchPerformedEvent event = new SearchPerformedEvent(
                "матча",
                3L,
                ProductCategory.TEA,
                List.of("Зелений чай"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(800),
                false,
                UUID.randomUUID(),
                "uk",
                "127.0.0.1",
                "Mozilla/5.0",
                OffsetDateTime.now()
        );

        searchAnalyticsEventListener.handleSearchPerformed(event);

        verify(searchAnalyticsService, times(1)).recordSearchEvent(event);
    }

    @Test
    @DisplayName("handleSearchPerformed catches exceptions and does not rethrow")
    void handleSearchPerformed_shouldSwallowExceptions() {
        SearchPerformedEvent event = new SearchPerformedEvent(
                "матча",
                0L,
                null,
                null,
                null,
                null,
                null,
                null,
                "uk",
                "127.0.0.1",
                "Mozilla/5.0",
                OffsetDateTime.now()
        );

        doThrow(new RuntimeException("DB error"))
                .when(searchAnalyticsService).recordSearchEvent(event);

        assertDoesNotThrow(() -> searchAnalyticsEventListener.handleSearchPerformed(event));
        verify(searchAnalyticsService).recordSearchEvent(event);
    }
}
