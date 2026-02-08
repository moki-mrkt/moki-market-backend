package ua.moki.modules.feedback.services;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import ua.moki.modules.feedback.dtos.FeedbackAnswerDTO;
import ua.moki.modules.feedback.dtos.FeedbackRequestDTO;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;
import ua.moki.modules.feedback.dtos.FeedbackUpdateDTO;

import java.math.BigDecimal;
import java.util.UUID;

public interface FeedbackService {

    FeedbackResponseDTO createFeedback(UUID userId, FeedbackRequestDTO dto);
    FeedbackResponseDTO updateFeedback(Long feedbackId, FeedbackUpdateDTO dto, Authentication authentication);
    FeedbackResponseDTO addAnswerToFeedback(Long feedbackId, String userRole, FeedbackAnswerDTO dto);
    void deleteFeedback(Long feedbackId, Authentication authentication);
    BigDecimal getAverageRatingForProduct(Long productId);
    BigDecimal getAverageRatingForStore();
    FeedbackResponseDTO getFeedbackById(Long feedbackId);
    Page<FeedbackResponseDTO> getFeedbacksByUserId(UUID userId, int page, int size);
    Page<FeedbackResponseDTO> getFeedbacksByProductId(Long productId, int page, int size);
    Page<FeedbackResponseDTO> getFeedbacksAboutStore(int page, int size);
    Page<FeedbackResponseDTO> getAllFeedbacks(int page, int size);
}
