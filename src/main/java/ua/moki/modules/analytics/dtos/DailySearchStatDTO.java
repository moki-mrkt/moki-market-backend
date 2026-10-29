package ua.moki.modules.analytics.dtos;

public record DailySearchStatDTO(
        String date,
        long totalSearches,
        long textSearches,
        long zeroResultSearches
) {
}
