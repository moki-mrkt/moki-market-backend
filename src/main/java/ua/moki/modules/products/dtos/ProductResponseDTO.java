package ua.moki.modules.products.dtos;

import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProductResponseDTO(
        Long id,
        String name,
        ProductCategory productCategory,
        String description,
        BigDecimal price,
        BigDecimal rating,
        ProductAvailability availability,
        Integer discount,
        String manufacturerOfTheProduct,
        String subcategory,
        String initOfMeasure,
        Integer valueOfInitOfMeasure,
        Long salesCount,
        OffsetDateTime creationTime,
        List<ProductImageDTO> images,
        Map<String, String> characteristics
) {
}
