package ua.moki.infrastructure.storage.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ua.moki.infrastructure.storage.dtos.CharacteristicsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedCharacteristicsDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedKeywordsDTO;
import ua.moki.infrastructure.storage.dtos.KeywordsGeneratorRequestDTO;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiProductGeneratorImplTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiProductGeneratorImpl generator;

    @BeforeEach
    void setUp() {
        generator = new GeminiProductGeneratorImpl(restTemplate);
        ReflectionTestUtils.setField(generator, "apiKey", "test-api-key");
    }

    @Test
    @DisplayName("generateCharacteristics parses UA and RU characteristics from Gemini JSON response")
    void generateCharacteristics_shouldParseJsonResponse() {
        String jsonPayload = """
                {
                  "characteristics": {
                    "Обробка": "Не солоні",
                    "Виробник": "Alesto",
                    "Пакування": "250г"
                  },
                  "characteristicsRu": {
                    "Упаковка": "250 г",
                    "Обработка": "Несоленые",
                    "Производитель": "Alesto"
                  },
                  "keywords": ["фісташки не солоні", "фісташки Alesto"],
                  "keywordsRu": ["фисташки несоленые", "фисташки Alesto"]
                }
                """;

        Map<String, Object> geminiApiResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", jsonPayload))
                        ))
                )
        );

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(geminiApiResponse);

        GeneratedCharacteristicsDTO result = generator.generateCharacteristics(
                new CharacteristicsGeneratorRequestDTO(
                        "Фісташки не солоні Alesto 250г",
                        "Горіхи",
                        "Фісташки",
                        "Alesto",
                        250,
                        "г"
                )
        );

        assertThat(result.characteristics())
                .containsEntry("Обробка", "Не солоні")
                .containsEntry("Виробник", "Alesto")
                .containsEntry("Пакування", "250г");

        assertThat(result.characteristicsRu())
                .containsEntry("Упаковка", "250 г")
                .containsEntry("Обработка", "Несоленые")
                .containsEntry("Производитель", "Alesto");

        assertThat(result.keywords()).containsExactly("фісташки не солоні", "фісташки Alesto");
        assertThat(result.keywordsRu()).containsExactly("фисташки несоленые", "фисташки Alesto");
    }

    @Test
    @DisplayName("generateCharacteristics falls back to deterministic UA and RU characteristics when Gemini fails")
    void generateCharacteristics_shouldFallbackOnError() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Network error"));

        GeneratedCharacteristicsDTO result = generator.generateCharacteristics(
                new CharacteristicsGeneratorRequestDTO(
                        "Горіхова суміш Alesto 500г",
                        "Горіхи",
                        "Горіхові суміші",
                        "Alesto",
                        500,
                        "г"
                )
        );

        assertThat(result.characteristics())
                .containsEntry("Виробник", "Alesto")
                .containsEntry("Пакування", "500г")
                .containsEntry("Вид", "Горіхові суміші");

        assertThat(result.characteristicsRu())
                .containsEntry("Производитель", "Alesto")
                .containsEntry("Упаковка", "500 г")
                .containsEntry("Вид", "Горіхові суміші");
    }

    @Test
    @DisplayName("generateKeywords parses UA and RU search queries from Gemini JSON response")
    void generateKeywords_shouldParseJsonResponse() {
        String jsonPayload = """
                {
                  "keywords": [
                    "фісташки не солоні",
                    "фісташки Alesto",
                    "купити фісташки"
                  ],
                  "keywordsRu": [
                    "фисташки несоленые",
                    "фисташки Alesto",
                    "купить фисташки"
                  ]
                }
                """;

        Map<String, Object> geminiApiResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", jsonPayload))
                        ))
                )
        );

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(geminiApiResponse);

        GeneratedKeywordsDTO result = generator.generateKeywords(
                new KeywordsGeneratorRequestDTO(
                        "Фісташки не солоні Alesto 250г",
                        "Горіхи",
                        "Фісташки",
                        "Alesto"
                )
        );

        assertThat(result.keywords()).containsExactly("фісташки не солоні", "фісташки Alesto", "купити фісташки");
        assertThat(result.keywordsRu()).containsExactly("фисташки несоленые", "фисташки Alesto", "купить фисташки");
    }
}
