package ua.moki.modules.feedback.dtos;

import java.time.OffsetDateTime;

public record ProductFeedbackResponseDTO(
        Long id,
        String comment,
        Integer rating,
        String firstNameUser,
        String secondNameUser,
        String userImageUrl,
        String productName,
        String productSlug,
        OffsetDateTime createdAt,
        String answer,
        OffsetDateTime answeredAt
) {
}
