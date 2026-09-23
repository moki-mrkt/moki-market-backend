package ua.moki.modules.products.dtos;

public record ProductVariantDTO(
        Long productId,
        String slug,
        String variantName,
        String variantValue,
        Boolean isCurrent
) {
}
