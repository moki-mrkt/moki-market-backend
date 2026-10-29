package ua.moki.modules.analytics.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.analytics.services.SearchAnalyticsService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAnalyticsEventListener {

    private final SearchAnalyticsService searchAnalyticsService;

    @Async("taskExecutor")
    @EventListener
    public void handleSearchPerformed(SearchPerformedEvent event) {
        try {
            searchAnalyticsService.recordSearchEvent(event);
        } catch (Exception e) {
            log.error("Failed to record search analytics event", e);
        }
    }
}
