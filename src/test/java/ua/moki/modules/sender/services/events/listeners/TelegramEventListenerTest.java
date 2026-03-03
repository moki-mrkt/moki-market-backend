package ua.moki.modules.sender.services.events.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.orders.dtos.OrderResponseDTO;

import ua.moki.modules.sender.services.TelegramSenderService;
import ua.moki.modules.sender.services.events.TelegramNewOrderEvent;
import ua.moki.modules.sender.services.listeners.TelegramEventListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramEventListenerTest {

    @Mock
    private TelegramSenderService telegramSender;

    @InjectMocks
    private TelegramEventListener telegramEventListener;

    @Test
    @DisplayName("Should call telegramSender when TelegramNewOrderEvent is handled")
    void handleNewOrderEvent_Success() {

        OrderResponseDTO orderResponse = mock(OrderResponseDTO.class);
        when(orderResponse.orderNumber()).thenReturn("ORD-123");
        TelegramNewOrderEvent event = new TelegramNewOrderEvent(orderResponse);

        telegramEventListener.handleEmailChange(event);

        verify(telegramSender, times(1)).sendMessageAboutNewOrder(orderResponse);
    }

    @Test
    @DisplayName("Should handle exception when telegramSender fails")
    void handleNewOrderEvent_ErrorHandling() {

        OrderResponseDTO orderResponse = mock(OrderResponseDTO.class);
        TelegramNewOrderEvent event = new TelegramNewOrderEvent(orderResponse);

        doThrow(new RuntimeException("Telegram API error"))
                .when(telegramSender).sendMessageAboutNewOrder(any());

        assertDoesNotThrow(() -> telegramEventListener.handleEmailChange(event));
        verify(telegramSender).sendMessageAboutNewOrder(orderResponse);
    }
}