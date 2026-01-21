package ua.moki.modules.feedback.dtos;

import java.time.OffsetDateTime;

public record FeedbackResponseDTO(
        Long id,
        String comment,
        Integer rating,
        String firstNameUser,
        String userImageUrl,
        OffsetDateTime createdAt,
        String answer,
        OffsetDateTime answeredAt
) {
}
