package ua.moki.modules.orders.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.moki.modules.orders.domains.CartItem;
import ua.moki.modules.products.domains.Product;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByProduct(Product product);
}
