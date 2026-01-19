package ua.moki.modules.orders.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.moki.modules.orders.domains.Order;

public interface OrderRepository  extends JpaRepository<Order, Long> {
}
