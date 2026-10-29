package ua.moki.modules.analytics.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.moki.modules.analytics.dtos.SearchAnalyticsDashboardDTO;
import ua.moki.modules.analytics.dtos.SearchLogResponseDTO;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.products.enums.ProductCategory;

public interface SearchAnalyticsService {

    void recordSearchEvent(SearchPerformedEvent event);

    SearchAnalyticsDashboardDTO getDashboardStats(int days);

    Page<SearchLogResponseDTO> getSearchLogs(
            String query,
            ProductCategory category,
            Boolean zeroResultsOnly,
            Boolean onlyTextQueries,
            Integer days,
            Pageable pageable
    );
}
