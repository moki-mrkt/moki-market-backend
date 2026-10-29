package ua.moki.modules.analytics.events;

import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SearchPerformedEvent(
        String query,
        long resultsCount,
        ProductCategory category,
        List<String> subcategories,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean hasDiscount,
        UUID userPublicId,
        String locale,
        String clientIp,
        String userAgent,
        OffsetDateTime occurredAt
) {
}
