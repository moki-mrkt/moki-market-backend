package ua.moki.infrastructure.storage.service;

import ua.moki.infrastructure.storage.dtos.CharacteristicsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedCharacteristicsDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedKeywordsDTO;
import ua.moki.infrastructure.storage.dtos.KeywordsGeneratorRequestDTO;

public interface GeminiProductGenerator {
    String generateDescription(String productName, String attributes);

    GeneratedCharacteristicsDTO generateCharacteristics(CharacteristicsGeneratorRequestDTO request);

    GeneratedKeywordsDTO generateKeywords(KeywordsGeneratorRequestDTO request);
}
