package ua.moki.modules.feedback.services.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ua.moki.modules.feedback.repositories.FeedbackRepository;
import ua.moki.modules.feedback.services.events.ProductRatingUpdateEvent;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRatingEventListener {

    private final ProductRepository productRepository;
    private final FeedbackRepository feedbackRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRatingUpdate(ProductRatingUpdateEvent event) {
        log.info("Starting rating recalculation for product ID: {}", event.productId());

        try {
            updateProductRating(event.productId());
        } catch (Exception e) {
            log.error("Error updating product rating for ID: {}", event.productId(), e);
        }
    }

    private void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found during rating update"));

        Double avgRating = feedbackRepository.getAverageRatingByProductId(productId);
        long count = feedbackRepository.countByProductId(productId);

        product.setReviewsCount(count);
        product.setRating(mapToRating(avgRating));

        productRepository.save(product);

        log.info("Product {} rating updated. New Rating: {}, Count: {}", productId, product.getRating(), count);
    }

    private BigDecimal mapToRating(Double average) {
        if (average == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
    }
}
