package ua.moki.modules.feedback.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.moki.modules.feedback.domains.Feedback;
import ua.moki.modules.users.domains.User;

import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM StoreFeedback f WHERE f.user = :user")
    boolean existsStoreFeedbackByUser(@Param("user") User user);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM ProductFeedback f WHERE f.user = :user AND f.product.id = :productId")
    boolean existsProductFeedbackByUserAndProductId(@Param("user") User user, @Param("productId") Long productId);

    @Query("SELECT f FROM StoreFeedback f WHERE f.user.publicId = :userId")
    Optional<Feedback> findStoreFeedbackByUserId(@Param("userId") UUID userId);

    @Query("SELECT f FROM ProductFeedback f WHERE f.user.publicId = :userId")
    Page<Feedback> findProductFeedbacksByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f FROM ProductFeedback f WHERE f.user.publicId = :userId AND f.product.id = :productId")
    Optional<Feedback> findUserFeedbackByUserIdAndProductId(@Param("userId") UUID userId, Long productId);

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
