package ua.moki.modules.orders.dtos;

import java.math.BigDecimal;

public record CartItemResponseDTO(
        Long productId,
        String productName,
        String productImage,
        BigDecimal currentPrice,
        BigDecimal productPrice,
        int quantity,
        BigDecimal totalPrice
) {}