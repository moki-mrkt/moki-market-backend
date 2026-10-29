package ua.moki.modules.analytics.dtos;

import java.util.List;

public record SearchAnalyticsDashboardDTO(
        long totalSearches,
        long textSearches,
        long catalogFilterSearches,
        long zeroResultSearches,
        double zeroResultRate,
        List<TopQueryStatDTO> topQueries,
        List<TopQueryStatDTO> topZeroResultQueries,
        List<CategorySearchStatDTO> topCategories,
        List<DailySearchStatDTO> dailyTrend
) {
}
