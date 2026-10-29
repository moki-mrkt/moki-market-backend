package ua.moki.modules.analytics.dtos;

import ua.moki.modules.products.enums.ProductCategory;

public record CategorySearchStatDTO(
        ProductCategory category,
        String categoryTitle,
        long searchesCount
) {
}
