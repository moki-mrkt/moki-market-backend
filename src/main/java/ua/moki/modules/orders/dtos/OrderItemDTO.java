package ua.moki.modules.orders.dtos;

import java.math.BigDecimal;

public record OrderItemDTO (
        Long productId,
        String itemName,
        BigDecimal finalPricePerUnit,
        int quantity,
        BigDecimal totalAmount
) {
}
