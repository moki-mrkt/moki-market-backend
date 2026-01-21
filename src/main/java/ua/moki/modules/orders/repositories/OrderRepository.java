package ua.moki.modules.orders.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ua.moki.modules.orders.domains.Order;
import ua.moki.modules.orders.dtos.OrderResponseDTO;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository  extends JpaRepository<Order, Long> {

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long getNextOrderNumber();

    Optional<Order> findOrderByPublicId(UUID publicId);

    Page<Order> findAll(Pageable pageable);
    Page<Order> findAllByUser_PublicId(UUID userId, Pageable pageable);
}
