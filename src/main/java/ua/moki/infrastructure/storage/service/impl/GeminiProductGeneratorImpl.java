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
                // Якщо помилка 503 (сервер перевантажений), робимо паузу і пробуємо знову
                if (e.getStatusCode().value() == 503) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        log.error("Gemini API перевантажений після {} спроб", maxRetries);
                        return "";
                    }
                    try {
                        Thread.sleep(2000); // Чекаємо 2 секунди перед наступною спробою
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
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

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
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
