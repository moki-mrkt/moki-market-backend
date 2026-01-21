package ua.moki.modules.orders.dtos;

import java.math.BigDecimal;

public record CartItemResponseDTO(
        Long productId,
        String productName,
        BigDecimal pricePerUnit,
        int quantity,
        BigDecimal totalPrice
) {}