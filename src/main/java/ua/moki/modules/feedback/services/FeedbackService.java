package ua.moki.modules.feedback.services;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import ua.moki.modules.feedback.dtos.*;

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
    FeedbackResponseDTO getUserFeedbackAboutStore(UUID userId);
    FeedbackResponseDTO getUserFeedbackAboutProduct(UUID userId, Long productId);
    Page<ProductFeedbackResponseDTO> getUserFeedbacksAboutProducts(UUID userId, int page, int size);
    Page<FeedbackResponseDTO> getFeedbacksByProductId(Long productId, int page, int size);
    Page<FeedbackResponseDTO> getFeedbacksAboutStore(int page, int size);
    Page<FeedbackResponseDTO> getAllFeedbacks(int page, int size);
}
