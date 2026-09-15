package ua.moki.infrastructure.storage.dtos;

import java.util.Map;

public record TranslatorDTO(
        String name,
        String subcategory,
        String description,
        Map<String, String> characteristics
) {
}
