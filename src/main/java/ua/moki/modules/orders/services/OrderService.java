package ua.moki.modules.orders.services;

import org.springframework.data.domain.Page;
import ua.moki.modules.orders.dtos.OrderRequestDTO;
import ua.moki.modules.orders.dtos.OrderResponseDTO;
import ua.moki.modules.orders.dtos.OrderUpdateDTO;

import java.util.UUID;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO dto);
    OrderResponseDTO updateOrder(UUID publicId, OrderUpdateDTO dto);
    void cancelOrder(UUID publicId);
    OrderResponseDTO getOrderByPublicId(UUID publicId);
    Page<OrderResponseDTO> getOrdersByUserId(UUID userId, int page, int size);
    Page<OrderResponseDTO> getAllOrders(int page, int size);
}
