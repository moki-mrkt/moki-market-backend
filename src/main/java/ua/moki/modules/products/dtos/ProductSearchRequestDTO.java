package ua.moki.modules.products.dtos;

import ua.moki.modules.products.enums.ProductCategory;

import java.util.List;

public record ProductSearchRequestDTO(
        String query,
        ProductCategory category,
        Double minPrice,
        Double maxPrice,
        List<String> subcategory,
        Boolean hasDiscount
) {
}
