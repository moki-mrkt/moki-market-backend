package ua.moki.modules.orders.services;

import ua.moki.modules.orders.dtos.CartResponseDTO;

import java.util.UUID;

public interface CartService {

    CartResponseDTO addToCart(UUID userId, Long productId, int quantity);
    void clearCart(UUID userId);
}
