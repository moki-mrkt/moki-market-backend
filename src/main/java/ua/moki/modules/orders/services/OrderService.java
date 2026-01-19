package ua.moki.modules.orders.services;

import org.springframework.security.core.Authentication;
import ua.moki.modules.orders.dtos.OrderRequestDTO;

import java.util.UUID;

public interface OrderService {
    void createOrder(OrderRequestDTO dto, Authentication authentication);
    void updateOrder();
    void deleteOrder();
    void getOrderByPublicId(UUID publicId);
    void getOrderByUserId(Long userId);
    void getAllOrders();
}
