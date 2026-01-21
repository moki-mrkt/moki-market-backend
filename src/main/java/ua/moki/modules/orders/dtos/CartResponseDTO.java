package ua.moki.modules.orders.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDTO(
        UUID cartId,
        BigDecimal totalCartPrice,
        List<CartItemResponseDTO> items
) {
}
