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
        String slug,
        ProductCategory productCategory,
        String description,
        BigDecimal price,
        BigDecimal priceWithDiscount,
        Integer discount,
        BigDecimal rating,
        Long reviewsCount,
        ProductAvailability availability,
        String manufacturerOfTheProduct,
        String subcategory,
        String initOfMeasure,
        Integer valueOfInitOfMeasure,
        boolean isFavorite,
        String productType,
        Integer minCustomWeight,
        Boolean allowCustomWeight,
        List<ProductWeightOptionDTO> weightOptions,
        List<ProductVariantDTO> siblingVariants,
        String groupId,
        String variantName,
        String variantValue,
        List<ProductImageResponseDTO> images,
        Map<String, String> characteristics
) {
}
