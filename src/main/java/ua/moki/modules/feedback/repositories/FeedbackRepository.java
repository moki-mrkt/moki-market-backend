package ua.moki.modules.feedback.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.moki.modules.feedback.domains.Feedback;

import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findAllByUser_PublicId(UUID userId, Pageable pageable);
    Page<Feedback> findAll(Pageable pageable);

    @Query("SELECT f FROM ProductFeedback f WHERE f.product.id = :productId")
    Page<Feedback> findAllByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT f FROM StoreFeedback f")
    Page<Feedback> findAllByStoreId(Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM ProductFeedback f WHERE f.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT AVG(f.rating) FROM StoreFeedback f")
    Double getAverageRatingForStore();

    @Query("SELECT COUNT(f) FROM ProductFeedback f WHERE f.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
}
