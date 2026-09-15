package ua.moki.infrastructure.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiProductGeneratorImpl implements GeminiProductGenerator {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public String generateDescription(String productName, String attributes) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;

        // Формируем промпт для генерации
        String prompt = String.format(
                "Ти досвідчений копірайтер інтернет-магазину. " +
                        "Напиши привабливий SEO-оптимізований опис для товару '%s'. " +
                        "Враховуй наступні характеристики: %s. " +
                        "Опис має бути без води, підкреслювати користь та смакові якості продукту, " +
                        "обсяг близько 500-700 символів.",
                productName, attributes
        );

        // Збираємо тіло запиту (JSON)
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Відправляємо запит
        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return extractTextFromResponse(response);
        } catch (Exception e) {
            // Логування помилки
            log.error(e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        // Парсинг JSON-відповіді Gemini
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts != null && !parts.isEmpty()) {
                return (String) parts.get(0).get("text");
            }
        }
        return "";
    }
}
