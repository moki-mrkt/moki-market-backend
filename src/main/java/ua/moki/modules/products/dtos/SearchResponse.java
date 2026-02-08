package ua.moki.modules.products.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchResponse(
        Page<ProductResponseDTO> products,
        List<String> subcategories,
        Double minPrice,
        Double maxPrice
) {
}
