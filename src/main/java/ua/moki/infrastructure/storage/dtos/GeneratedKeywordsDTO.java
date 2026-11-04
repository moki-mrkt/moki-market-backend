package ua.moki.infrastructure.storage.dtos;

import java.util.List;

public record GeneratedKeywordsDTO(
        List<String> keywords,
        List<String> keywordsRu
) {
}
