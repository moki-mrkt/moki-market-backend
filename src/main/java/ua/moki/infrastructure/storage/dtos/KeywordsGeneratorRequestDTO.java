package ua.moki.infrastructure.storage.dtos;

public record KeywordsGeneratorRequestDTO(
        String nameProduct,
        String category,
        String subcategory,
        String manufacturer
) {
}
