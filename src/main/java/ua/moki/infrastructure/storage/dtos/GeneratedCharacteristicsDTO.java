package ua.moki.infrastructure.storage.dtos;

import java.util.List;
import java.util.Map;

public record GeneratedCharacteristicsDTO(
        Map<String, String> characteristics,
        Map<String, String> characteristicsRu,
        List<String> keywords,
        List<String> keywordsRu
) {
    public GeneratedCharacteristicsDTO(
            Map<String, String> characteristics,
            Map<String, String> characteristicsRu
    ) {
        this(characteristics, characteristicsRu, List.of(), List.of());
    }
}
