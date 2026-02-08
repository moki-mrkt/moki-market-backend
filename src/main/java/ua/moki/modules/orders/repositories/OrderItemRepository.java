package ua.moki.modules.orders.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.moki.modules.orders.domains.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);
}
