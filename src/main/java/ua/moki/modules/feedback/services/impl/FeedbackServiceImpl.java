package ua.moki.modules.feedback.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.feedback.domains.Feedback;
import ua.moki.modules.feedback.domains.ProductFeedback;
import ua.moki.modules.feedback.domains.StoreFeedback;
import ua.moki.modules.feedback.dtos.FeedbackAnswerDTO;
import ua.moki.modules.feedback.dtos.FeedbackRequestDTO;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;
import ua.moki.modules.feedback.dtos.FeedbackUpdateDTO;
import ua.moki.modules.feedback.repositories.FeedbackRepository;
import ua.moki.modules.feedback.services.FeedbackService;
import ua.moki.modules.feedback.services.events.ProductRatingUpdateEvent;
import ua.moki.modules.feedback.utils.FeedbackMapper;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.services.UserService;
import ua.moki.util.exceptions.AlreadyExistsException;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedbackServiceImpl implements FeedbackService {

    UserService userService;
    ProductService productService;
    FeedbackRepository feedbackRepository;
    FeedbackMapper feedbackMapper;

    ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FeedbackResponseDTO createFeedback(UUID userId, FeedbackRequestDTO dto) {

        User user = userService.getActiveUserEntityByPublicId(userId);

        if (dto.productId() != null && feedbackRepository.existsProductFeedbackByUserAndProductId(user, dto.productId())) {
            throw new AlreadyExistsException("Ви вже залишили відгук про цей товар");
        } else if (dto.productId() == null && feedbackRepository.existsStoreFeedbackByUser(user)) {
            throw new AlreadyExistsException("Ви вже залишили відгук про наш магазин");
        }

        Feedback feedback = dto.productId() != null ? createProductFeedback(dto.productId()) : new StoreFeedback();

        feedback.setUser(user);
        feedback.setComment(dto.comment());
        feedback.setRating(dto.rating());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        if (savedFeedback instanceof ProductFeedback productFeedback) {
            Long productId = productFeedback.getProduct().getId();
            eventPublisher.publishEvent(new ProductRatingUpdateEvent(productId));
        }

        return feedbackMapper.toDto(savedFeedback);
    }

    private ProductFeedback createProductFeedback(Long productId) {
        Product product = productService.findById(productId);

        ProductFeedback productFeedback = new ProductFeedback();
        productFeedback.setProduct(product);

        return productFeedback;
    }

    @Override
    @Transactional
    public FeedbackResponseDTO updateFeedback(Long feedbackId, FeedbackUpdateDTO dto, Authentication authentication) {

        Feedback feedback = findFeedbackById(feedbackId);

        checkFeedbackAccess(feedback, authentication);

        feedback.setComment(dto.comment());
        feedback.setRating(dto.rating());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        return feedbackMapper.toDto(savedFeedback);
    }

    @Override
    @Transactional
    public FeedbackResponseDTO addAnswerToFeedback(Long feedbackId, String userRole, FeedbackAnswerDTO dto) {

        Feedback feedback = findFeedbackById(feedbackId);

        boolean isAdmin = userRole.equals("ROLE_ADMIN") || userRole.equals("ROLE_MANAGER");

        if (!isAdmin) {
            throw new AccessDeniedException("You don't have permission to add answer for this feedback");
        }

        feedback.setAnswer(dto.answer());
        feedback.setAnsweredAt(OffsetDateTime.now());

        Feedback updatedFeedback = feedbackRepository.save(feedback);

        return feedbackMapper.toDto(updatedFeedback);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long feedbackId, Authentication authentication) {

        Feedback feedback = findFeedbackById(feedbackId);

        checkFeedbackAccess(feedback, authentication);

        Long productId = null;
        if (feedback instanceof ProductFeedback pf) {
            productId = pf.getProduct().getId();
        }

        feedbackRepository.delete(feedback);

        if (productId != null) {
            eventPublisher.publishEvent(new ProductRatingUpdateEvent(productId));
        }
    }

    private void checkFeedbackAccess(Feedback feedback, Authentication authentication) {

        UUID currentUserId = UUID.fromString(authentication.getName());
        String userRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (userRole == null) {
            throw new AccessDeniedException("User role is null");
        }

        boolean isOwner = feedback.getUser().getPublicId().equals(currentUserId);
        boolean isAdmin = userRole.equals("ROLE_ADMIN") || userRole.equals("ROLE_MANAGER");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You don't have permission to delete this feedback");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageRatingForProduct(Long productId) {
        Double average = feedbackRepository.getAverageRatingByProductId(productId);
        return mapToRating(average);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageRatingForStore() {
        Double average = feedbackRepository.getAverageRatingForStore();
        return mapToRating(average);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponseDTO getFeedbackById(Long feedbackId) {
        return feedbackMapper.toDto(findFeedbackById(feedbackId));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponseDTO getUserFeedbackAboutStore(UUID userId) {
        Feedback feedback = feedbackRepository.findStoreFeedbackByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found"));

        return feedbackMapper.toDto(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponseDTO getUserFeedbackAboutProduct(UUID userId, Long productId) {
        Feedback feedback = feedbackRepository.findUserFeedbackByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found"));

        return feedbackMapper.toDto(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponseDTO> getUserFeedbacksAboutProducts(UUID userId, int page, int size) {
        return feedbackRepository.findProductFeedbacksByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(feedbackMapper::toDto);
    }

    private Feedback findFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponseDTO> getFeedbacksByProductId(Long productId, int page, int size) {
        return feedbackRepository.findAllByProductId(productId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(feedbackMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponseDTO> getFeedbacksAboutStore(int page, int size) {
        return feedbackRepository.findAllByStoreId(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(feedbackMapper::toDto);
    }

    @Override
    public Page<FeedbackResponseDTO> getAllFeedbacks(int page, int size) {
        return feedbackRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(feedbackMapper::toDto);
    }

    private BigDecimal mapToRating(Double average) {
        if (average == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(average)
                .setScale(1, RoundingMode.HALF_UP);
    }
}
