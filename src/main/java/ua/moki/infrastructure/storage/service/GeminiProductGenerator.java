package ua.moki.infrastructure.storage.service;

import ua.moki.infrastructure.storage.dtos.CharacteristicsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedCharacteristicsDTO;

public interface GeminiProductGenerator {
    String generateDescription(String productName, String attributes);

    GeneratedCharacteristicsDTO generateCharacteristics(CharacteristicsGeneratorRequestDTO request);
}
