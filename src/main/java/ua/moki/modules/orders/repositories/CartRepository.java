package ua.moki.modules.orders.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.moki.modules.orders.domains.Cart;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findCartByUser_PublicId(UUID userId);
    void deleteAllByUpdatedAtBefore(OffsetDateTime threshold);
}
