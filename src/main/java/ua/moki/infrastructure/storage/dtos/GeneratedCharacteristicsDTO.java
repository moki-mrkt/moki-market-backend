package ua.moki.infrastructure.storage.dtos;

import java.util.Map;

public record GeneratedCharacteristicsDTO(
        Map<String, String> characteristics,
        Map<String, String> characteristicsRu
) {
}
