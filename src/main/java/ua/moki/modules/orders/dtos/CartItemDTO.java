package ua.moki.modules.orders.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemDTO(
        @NotNull(message = "Product ID is required")
        Long productId,
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Too many items") // Захист від "дурня" (DDOS або помилки)
        int quantity
) {
}
