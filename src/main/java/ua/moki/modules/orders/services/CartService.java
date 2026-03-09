package ua.moki.modules.orders.services;

import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.users.domains.User;

import java.util.UUID;

public interface CartService {

    CartResponseDTO addToCart(UUID userId, Long productId, int quantity);
    CartResponseDTO updateItemQuantity(UUID userId, Long productId, int quantity);
    CartResponseDTO deleteItemFromCart(UUID userId, Long productId);
    void clearCart(User user);

    CartResponseDTO getCart(UUID userId);
}
