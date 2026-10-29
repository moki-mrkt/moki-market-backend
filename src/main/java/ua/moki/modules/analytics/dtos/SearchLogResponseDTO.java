package ua.moki.modules.analytics.dtos;

import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SearchLogResponseDTO(
        Long id,
        String query,
        String normalizedQuery,
        Long resultsCount,
        Boolean zeroResults,
        ProductCategory category,
        String categoryTitle,
        String subcategories,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean hasDiscount,
        UUID userPublicId,
        String locale,
        String clientIp,
        OffsetDateTime createdAt
) {
}
