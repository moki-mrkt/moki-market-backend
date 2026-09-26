package ua.moki.modules.products.dtos;

import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProductAdminResponseDTO(
        Long id,
        String name,
        String nameRu,
        ProductCategory productCategory,
        String description,
        String descriptionRu,
        BigDecimal price,
        BigDecimal priceWithDiscount,
        BigDecimal purchasePrice,
        Integer discount,
        BigDecimal rating,
        Long reviewsCount,
        ProductAvailability availability,
        String manufacturerOfTheProduct,
        String subcategory,
        String subcategoryRu,
        String initOfMeasure,
        Integer valueOfInitOfMeasure,
        Long salesCount,
        String productType,
        Integer minCustomWeight,
        Boolean allowCustomWeight,
        List<ProductWeightOptionDTO> weightOptions,
        List<ProductVariantDTO> siblingVariants,
        String groupId,
        String variantName,
        String variantValue,
        OffsetDateTime creationTime,
        List<ProductImageResponseDTO> images,
        Map<String, String> characteristics,
        Map<String, String> characteristicsRu
) {
}
