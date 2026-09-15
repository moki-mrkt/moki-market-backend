package ua.moki.infrastructure.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.moki.infrastructure.storage.dtos.TranslatorDTO;
import ua.moki.infrastructure.storage.service.TranslatorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslatorServiceImpl implements TranslatorService {

    private final RestTemplate restTemplate;

    private static final String DEEPL_URL = "https://api-free.deepl.com/v2/translate";
    private static final String API_KEY = "bded547c-6f88-4fc4-8030-b4ccf1a3541e:fx";

    @Override
    public TranslatorDTO translateTextsToRu(TranslatorDTO translateUaDTO) {
        if (translateUaDTO == null) {
            return null;
        }

        // 1. Пакуємо всі текстові поля в один список
        List<String> textsToTranslate = new ArrayList<>();
        textsToTranslate.add(translateUaDTO.name() != null ? translateUaDTO.name() : "");
        textsToTranslate.add(translateUaDTO.subcategory() != null ? translateUaDTO.subcategory() : "");
        textsToTranslate.add(translateUaDTO.description() != null ? translateUaDTO.description() : "");

        // Пакуємо характеристики (і ключі, і значення, щоб перекласти все)
        if (translateUaDTO.characteristics() != null) {
            for (Map.Entry<String, String> entry : translateUaDTO.characteristics().entrySet()) {
                textsToTranslate.add(entry.getKey() != null ? entry.getKey() : "");
                textsToTranslate.add(entry.getValue() != null ? entry.getValue() : "");
            }
        }

        // 2. Формуємо безпечне тіло запиту через Map
        Map<String, Object> requestBody = Map.of(
                "text", textsToTranslate,
                "source_lang", "UK",
                "target_lang", "RU"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "DeepL-Auth-Key " + API_KEY);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(DEEPL_URL, request, Map.class);
            List<String> translatedTexts = extractTextFromResponse(response);

            // Якщо щось пішло не так і масиви не збігаються, повертаємо оригінал
            if (translatedTexts.isEmpty() || translatedTexts.size() != textsToTranslate.size()) {
                log.warn("DeepL повернув некоректну кількість перекладів.");
                return translateUaDTO;
            }

            // 4. Розпаковуємо перекладений список назад у DTO
            String nameRu = translatedTexts.get(0);
            String subcategoryRu = translatedTexts.get(1);
            String descriptionRu = translatedTexts.get(2);

            Map<String, String> charsRu = new HashMap<>();
            int index = 3;
            if (translateUaDTO.characteristics() != null) {
                for (int i = 0; i < translateUaDTO.characteristics().size(); i++) {
                    String keyRu = translatedTexts.get(index++);
                    String valRu = translatedTexts.get(index++);
                    charsRu.put(keyRu, valRu);
                }
            }

            return new TranslatorDTO(nameRu, subcategoryRu, descriptionRu, charsRu);

        } catch (Exception e) {
            log.error("Помилка під час перекладу: {}", e.getMessage());
            // У разі помилки зв'язку з DeepL повертаємо оригінальний об'єкт, щоб не зламати фронтенд
            return translateUaDTO;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTextFromResponse(Map<String, Object> response) {
        List<String> result = new ArrayList<>();
        if (response == null || !response.containsKey("translations")) {
            return result;
        }

        List<Map<String, Object>> translations = (List<Map<String, Object>>) response.get("translations");
        for (Map<String, Object> translation : translations) {
            result.add((String) translation.get("text"));
        }

        return result;
    }
}
