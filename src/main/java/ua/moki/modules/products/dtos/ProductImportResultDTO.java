package ua.moki.modules.products.dtos;

import java.util.List;

public record ProductImportResultDTO(
        int totalRows,
        int importedCount,
        int skippedCount,
        List<ProductImportErrorDTO> errors
) {
}
