package ua.moki.modules.analytics.controllers;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.moki.modules.analytics.dtos.SearchAnalyticsDashboardDTO;
import ua.moki.modules.analytics.dtos.SearchLogResponseDTO;
import ua.moki.modules.analytics.services.SearchAnalyticsService;
import ua.moki.modules.products.enums.ProductCategory;

@RestController
@RequestMapping("/analytics/search")
@RequiredArgsConstructor
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SearchAnalyticsDashboardDTO> getDashboard(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        return ResponseEntity.ok(searchAnalyticsService.getDashboardStats(days));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<SearchLogResponseDTO>> getSearchLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) Boolean zeroResultsOnly,
            @RequestParam(required = false) Boolean onlyTextQueries,
            @RequestParam(required = false) Integer days,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SearchLogResponseDTO> logs = searchAnalyticsService.getSearchLogs(
                query,
                category,
                zeroResultsOnly,
                onlyTextQueries,
                days,
                pageable
        );
        return ResponseEntity.ok(logs);
    }
}
