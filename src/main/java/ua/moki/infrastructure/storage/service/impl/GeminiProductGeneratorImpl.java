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
import ua.moki.infrastructure.storage.dtos.GeneratedKeywordsDTO;
import ua.moki.infrastructure.storage.dtos.KeywordsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;

import java.util.ArrayList;
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
            return new GeneratedCharacteristicsDTO(Map.of(), Map.of(), List.of(), List.of());
        }

        String measureStr = formatMeasure(req.measureValue(), req.measureUnit());

        String prompt = String.format(
                """
                Ти контент-менеджер інтернет-магазину продуктів та смаколиків Moki.
                Згенеруй лаконічний набір із 3-4 характеристик товару українською (characteristics) та російською (characteristicsRu) мовами, а також по 8-10 релевантних пошукових запитів для маркетплейсу Prom.ua та пошуку на сайті українською (keywords) і російською (keywordsRu).
                
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
                2. Значення характеристик мають бути короткими (1-3 слова), з великої літери.
                3. Згенеруй 8-10 пошукових запитів у масивах "keywords" (UA) та "keywordsRu" (RU): короткі комерційні та інформаційні фрази по 1-4 слова, за якими покупці шукають цей товар на Prom.ua та на сайті.
                4. Приклад еталонного результату:
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
                     "keywords": [
                       "фісташки не солоні",
                       "фісташки Alesto",
                       "фісташки 250г",
                       "купити фісташки",
                       "горіхи фісташки",
                       "фісташки без солі",
                       "смажені фісташки",
                       "горішки до снеків"
                     ],
                     "keywordsRu": [
                       "фисташки несоленые",
                       "фисташки Alesto",
                       "фисташки 250г",
                       "купить фисташки",
                       "орехи фисташки",
                       "фисташки без соли",
                       "жареные фисташки",
                       "орешки к снекам"
                     ]
                   }
                5. Поверни ВИКЛЮЧНО валідний JSON об'єкт без markdown-обгорток з чотирма полями: "characteristics", "characteristicsRu", "keywords", "keywordsRu".
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

    @Override
    public GeneratedKeywordsDTO generateKeywords(KeywordsGeneratorRequestDTO req) {
        if (req == null || req.nameProduct() == null || req.nameProduct().isBlank()) {
            return new GeneratedKeywordsDTO(List.of(), List.of());
        }

        String prompt = String.format(
                """
                Ти SEO-спеціаліст та контент-менеджер інтернет-магазину продуктів і смаколиків Moki.
                Згенеруй по 10-12 релевантних пошукових запитів (ключових фраз) українською (keywords) та російською (keywordsRu) мовами для картки товару на Prom.ua (блок "Пошукові запити", де рекомендовано мінімум 8 запитів) та для внутрішнього пошуку на сайті.
                
                Вхідні дані про товар:
                - Назва: %s
                - Категорія: %s
                - Підкатегорія: %s
                - Виробник: %s
                
                Правила:
                1. Кожен запит — це реальна пошукова фраза з 1-4 слів, яку вводять покупці (з малої літери, крім власних назв брендів).
                2. Включи:
                   - прямі назви товару та синоніми;
                   - комбінації з виробником/брендом (якщо вказаний);
                   - комбінації з категорією/підкатегорією та вагою/фасуванням (якщо є в назві);
                   - комерційні запити ("купити ...", "... ціна", "натуральні ..." тощо).
                3. Не дублюй однакові фрази.
                4. Приклад еталонного результату для товару "Фісташки не солоні Alesto 250г":
                   {
                     "keywords": [
                       "фісташки не солоні",
                       "фісташки Alesto",
                       "фісташки 250г",
                       "купити фісташки",
                       "горіхи фісташки",
                       "фісташки без солі",
                       "смажені фісташки",
                       "фісташки Alesto 250 г",
                       "натуральні фісташки",
                       "горішки фісташки"
                     ],
                     "keywordsRu": [
                       "фисташки несоленые",
                       "фисташки Alesto",
                       "фисташки 250г",
                       "купить фисташки",
                       "орехи фисташки",
                       "фисташки без соли",
                       "жареные фисташки",
                       "фисташки Alesto 250 г",
                       "натуральные фисташки",
                       "орешки фисташки"
                     ]
                   }
                5. Поверни ВИКЛЮЧНО валідний JSON об'єкт без markdown-обгорток з двома масивами рядків: "keywords" та "keywordsRu".
                """,
                safeStr(req.nameProduct()),
                safeStr(req.category()),
                safeStr(req.subcategory()),
                safeStr(req.manufacturer())
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
        GeneratedKeywordsDTO parsed = parseKeywordsJson(rawResponse);

        if (parsed != null && !parsed.keywords().isEmpty()) {
            return parsed;
        }

        return buildFallbackKeywords(req);
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
            List<String> keywordsUa = toStringList(root.get("keywords"));
            List<String> keywordsRu = toStringList(root.get("keywordsRu"));

            if (ua.isEmpty()) {
                return null;
            }
            return new GeneratedCharacteristicsDTO(
                    ua,
                    ru.isEmpty() ? new LinkedHashMap<>(ua) : ru,
                    keywordsUa,
                    keywordsRu.isEmpty() ? new ArrayList<>(keywordsUa) : keywordsRu
            );
        } catch (Exception e) {
            log.warn("Не вдалося розпарсити JSON характеристик від Gemini: {}", e.getMessage());
            return null;
        }
    }

    private GeneratedKeywordsDTO parseKeywordsJson(String rawText) {
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
            List<String> keywordsUa = toStringList(root.get("keywords"));
            List<String> keywordsRu = toStringList(root.get("keywordsRu"));

            if (keywordsUa.isEmpty()) {
                return null;
            }
            return new GeneratedKeywordsDTO(
                    keywordsUa,
                    keywordsRu.isEmpty() ? new ArrayList<>(keywordsUa) : keywordsRu
            );
        } catch (Exception e) {
            log.warn("Не вдалося розпарсити JSON пошукових запитів від Gemini: {}", e.getMessage());
            return null;
        }
    }

    private GeneratedKeywordsDTO buildFallbackKeywords(KeywordsGeneratorRequestDTO req) {
        List<String> fallbackUa = new ArrayList<>();
        if (req.nameProduct() != null && !req.nameProduct().isBlank()) {
            String name = req.nameProduct().trim();
            fallbackUa.add(name);
            fallbackUa.add("купити " + name.toLowerCase());
        }
        if (req.subcategory() != null && !req.subcategory().isBlank()) {
            fallbackUa.add(req.subcategory().trim().toLowerCase());
        }
        if (req.category() != null && !req.category().isBlank()) {
            fallbackUa.add(req.category().trim().toLowerCase());
        }
        if (req.manufacturer() != null && !req.manufacturer().isBlank() && req.nameProduct() != null) {
            fallbackUa.add(req.nameProduct().trim() + " " + req.manufacturer().trim());
        }
        return new GeneratedKeywordsDTO(fallbackUa, new ArrayList<>(fallbackUa));
    }

    private List<String> toStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String s = String.valueOf(item).trim();
                    if (!s.isEmpty() && !result.contains(s)) {
                        result.add(s);
                    }
                }
            }
        }
        return result;
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

        List<String> fallbackKeywords = new ArrayList<>();
        if (req.nameProduct() != null && !req.nameProduct().isBlank()) {
            fallbackKeywords.add(req.nameProduct().trim());
        }
        if (req.subcategory() != null && !req.subcategory().isBlank()) {
            fallbackKeywords.add(req.subcategory().trim());
        }
        if (req.manufacturer() != null && !req.manufacturer().isBlank() && req.nameProduct() != null) {
            fallbackKeywords.add(req.nameProduct().trim() + " " + req.manufacturer().trim());
        }

        return new GeneratedCharacteristicsDTO(ua, ru, fallbackKeywords, new ArrayList<>(fallbackKeywords));
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
