package ua.moki.modules.sender.services;

import ua.moki.modules.orders.dtos.OrderResponseDTO;

import java.io.File;

public interface TelegramSenderService {

    void sendMessageAboutNewOrder(OrderResponseDTO responseDTO);
    void sendPhotoToTelegram(File photo);
}
