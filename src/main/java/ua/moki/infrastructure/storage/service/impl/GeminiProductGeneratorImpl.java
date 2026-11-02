package ua.moki.infrastructure.storage.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.moki.infrastructure.storage.dtos.CharacteristicsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedCharacteristicsDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiProductGeneratorImpl implements GeminiProductGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Override
    public String generateDescription(String productName, String attributes) {
        String prompt = String.format(
                "Ти досвідчений копірайтер інтернет-магазину. " +
                        "Напиши привабливий SEO-оптимізований опис для товару '%s'. " +
                        "Враховуй наступні характеристики: %s. " +
                        "Опис має бути без води, підкреслювати користь та смакові якості продукту, " +
                        "обсяг близько 500-700 символів.",
                productName, attributes
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        return callGemini(requestBody);
    }

    @Override
    public GeneratedCharacteristicsDTO generateCharacteristics(CharacteristicsGeneratorRequestDTO req) {
        if (req == null || req.nameProduct() == null || req.nameProduct().isBlank()) {
            return new GeneratedCharacteristicsDTO(Map.of(), Map.of());
        }

        String measureStr = formatMeasure(req.measureValue(), req.measureUnit());

        String prompt = String.format(
                """
                Ти контент-менеджер інтернет-магазину продуктів та смаколиків Moki.
                Згенеруй лаконічний набір із 3-4 характеристик товару українською (characteristics) та російською (characteristicsRu) мовами.
                
                Вхідні дані про товар:
                - Назва: %s
                - Категорія: %s
                - Підкатегорія: %s
                - Виробник: %s
                - Фасування / Вага / Об'єм: %s
                
                Правила:
                1. Згенеруй 3-4 найважливіші характеристики точно за стилем прикладів:
                   - Обов'язково включи "Виробник" (UA) / "Производитель" (RU), якщо виробник відомий.
                   - Обов'язково включи "Пакування" або "Фасування" (UA) / "Упаковка" або "Фасовка" (RU), якщо відомий розмір/вага (наприклад: "250г", "500г", "1кг").
                   - Додай 1-2 ключові характеристики з назви або підкатегорії товару (наприклад: "Обробка": "Не солоні" / "Смажені" / "Сирі", "Тип горіху": "Суміш" / "Фісташки" / "Кеш'ю", "Вид", "Смак", "Тип" тощо).
                2. Значення мають бути короткими (1-3 слова), з великої літери.
                3. Приклади еталонного результату:
                   Приклад 1 (Фісташки не солоні Alesto 250г):
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
                     }
                   }
                   Приклад 2 (Горіхова суміш Alesto 500г):
                   {
                     "characteristics": {
                       "Виробник": "Alesto",
                       "Фасування": "500г",
                       "Тип горіху": "Суміш"
                     },
                     "characteristicsRu": {
                       "Фасовка": "500 г",
                       "Тип ореха": "Смесь",
                       "Производитель": "Alesto"
                     }
                   }
                4. Поверни ВИКЛЮЧНО валідний JSON об'єкт без markdown-обгорток з двома полями: "characteristics" та "characteristicsRu".
                """,
                safeStr(req.nameProduct()),
                safeStr(req.category()),
                safeStr(req.subcategory()),
                safeStr(req.manufacturer()),
                measureStr
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        String rawResponse = callGemini(requestBody);
        GeneratedCharacteristicsDTO parsed = parseCharacteristicsJson(rawResponse);

        if (parsed != null && !parsed.characteristics().isEmpty()) {
            return parsed;
        }

        return buildFallbackCharacteristics(req, measureStr);
    }

    private String callGemini(Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
                return extractTextFromResponse(response);
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                if (e.getStatusCode().value() == 503) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        log.error("Gemini API перевантажений після {} спроб", maxRetries);
                        return "";
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "";
                    }
                } else {
                    log.error("Помилка API: {}", e.getMessage());
                    return "";
                }
            } catch (Exception e) {
                log.error("Неочікувана помилка: {}", e.getMessage());
                return "";
            }
        }
        return "";
    }

    private GeneratedCharacteristicsDTO parseCharacteristicsJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        try {
            String cleaned = rawText.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "")
                        .replaceFirst("\\s*```$", "")
                        .trim();
            }

            Map<String, Object> root = OBJECT_MAPPER.readValue(cleaned, new TypeReference<>() {});
            Map<String, String> ua = toStringMap(root.get("characteristics"));
            Map<String, String> ru = toStringMap(root.get("characteristicsRu"));

            if (ua.isEmpty()) {
                return null;
            }
            return new GeneratedCharacteristicsDTO(ua, ru.isEmpty() ? new LinkedHashMap<>(ua) : ru);
        } catch (Exception e) {
            log.warn("Не вдалося розпарсити JSON характеристик від Gemini: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> toStringMap(Object obj) {
        Map<String, String> result = new LinkedHashMap<>();
        if (obj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String k = String.valueOf(entry.getKey()).trim();
                    String v = String.valueOf(entry.getValue()).trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        result.put(k, v);
                    }
                }
            }
        }
        return result;
    }

    private GeneratedCharacteristicsDTO buildFallbackCharacteristics(CharacteristicsGeneratorRequestDTO req, String measureStr) {
        Map<String, String> ua = new LinkedHashMap<>();
        Map<String, String> ru = new LinkedHashMap<>();

        if (req.manufacturer() != null && !req.manufacturer().isBlank()) {
            ua.put("Виробник", req.manufacturer().trim());
            ru.put("Производитель", req.manufacturer().trim());
        }
        if (!measureStr.isBlank()) {
            ua.put("Пакування", measureStr.replace(" ", ""));
            ru.put("Упаковка", measureStr);
        }
        if (req.subcategory() != null && !req.subcategory().isBlank()) {
            ua.put("Вид", req.subcategory().trim());
            ru.put("Вид", req.subcategory().trim());
        }

        return new GeneratedCharacteristicsDTO(ua, ru);
    }

    private String formatMeasure(Integer value, String unit) {
        if (value == null || value <= 0) {
            return "";
        }
        String u = (unit == null || unit.isBlank() || "-".equals(unit.trim())) ? "г" : unit.trim();
        return value + " " + u;
    }

    private String safeStr(String s) {
        return s == null ? "" : s.trim();
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        return "";
    }
}
