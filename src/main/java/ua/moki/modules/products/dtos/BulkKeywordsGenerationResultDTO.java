package ua.moki.modules.products.dtos;

public record BulkKeywordsGenerationResultDTO(
        int totalProducts,
        int updatedProducts,
        int skippedProducts,
        int failedProducts
) {
}
