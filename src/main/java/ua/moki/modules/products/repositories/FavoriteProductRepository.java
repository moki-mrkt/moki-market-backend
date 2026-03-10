package ua.moki.modules.products.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.moki.modules.products.domains.FavoriteProduct;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.users.domains.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {

    Page<FavoriteProduct> findByUser_PublicId(UUID userId, Pageable pageable);

    @Query("SELECT f.product.id FROM FavoriteProduct f WHERE f.user.publicId = :userId")
    Set<Long> findProductIdsByUserPublicId(@Param("userId") UUID userId);

    Optional<FavoriteProduct> findByUser_PublicIdAndProductId(UUID userId, long productId);

    boolean existsByUser_PublicIdAndProductId(UUID userId, Long productId);
}
