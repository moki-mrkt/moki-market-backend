package ua.moki.modules.analytics.dtos;

public record TopQueryStatDTO(
        String query,
        long searchesCount,
        double avgResultsCount,
        long zeroResultsCount
) {
}
