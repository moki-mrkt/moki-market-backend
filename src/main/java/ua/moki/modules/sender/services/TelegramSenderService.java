package ua.moki.modules.sender.services;

import ua.moki.modules.orders.dtos.OrderResponseDTO;

public interface TelegramSenderService {

    void sendMessageAboutNewOrder(OrderResponseDTO responseDTO);
}
