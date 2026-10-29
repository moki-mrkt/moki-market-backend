package ua.moki.modules.products.dtos;

public record ProductImportErrorDTO(
        int rowNumber,
        String productName,
        String message
) {
}
