package ua.moki.modules.feedback.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record FeedbackStoreResponseDTO(
        BigDecimal storeRating,
        Page<FeedbackResponseDTO> feedbacks
) {
}
