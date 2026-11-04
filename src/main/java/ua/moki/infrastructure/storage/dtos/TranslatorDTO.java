package ua.moki.infrastructure.storage.dtos;

import java.util.List;
import java.util.Map;

public record TranslatorDTO(
        String name,
        String subcategory,
        String description,
        Map<String, String> characteristics,
        List<String> keywords
) {
    public TranslatorDTO(
            String name,
            String subcategory,
            String description,
            Map<String, String> characteristics
    ) {
        this(name, subcategory, description, characteristics, List.of());
    }
}
