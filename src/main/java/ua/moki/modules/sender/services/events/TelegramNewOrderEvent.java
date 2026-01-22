package ua.moki.modules.sender.services.events;

import ua.moki.modules.orders.dtos.OrderResponseDTO;

public record TelegramNewOrderEvent(OrderResponseDTO responseDTO) {
}
