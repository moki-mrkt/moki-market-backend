package ua.moki.infrastructure.storage.dtos;

public record CharacteristicsGeneratorRequestDTO(
        String nameProduct,
        String category,
        String subcategory,
        String manufacturer,
        Integer measureValue,
        String measureUnit
) {
}
