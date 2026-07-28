package ua.moki.modules.sender.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.moki.configuration.TelegramConfig;
import ua.moki.modules.orders.dtos.AddressDTO;
import ua.moki.modules.orders.dtos.OrderItemDTO;
import ua.moki.modules.orders.dtos.OrderResponseDTO;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.enums.PaymentStatus;
import ua.moki.modules.orders.utils.enums.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramSenderServiceImplTest {

    @Mock
    private TelegramConfig telegramConfig;

    @Spy
    private DefaultBotOptions options = new DefaultBotOptions();

    private TelegramSenderServiceImpl telegramSenderService;

    @BeforeEach
    void setUp() {
        telegramSenderService = spy(new TelegramSenderServiceImpl(options, "fake-token", telegramConfig));
    }

    @Test
    @DisplayName("Should correctly format and send telegram message about new order")
    void sendMessageAboutNewOrder_shouldSendCorrectFormattedMessage() throws TelegramApiException {

        String chatId = "123456";
        when(telegramConfig.getOrderChatId()).thenReturn(chatId);


        OrderResponseDTO order = createSimpleOrder();

        doReturn(null).when(telegramSenderService).execute(any(SendMessage.class));

        telegramSenderService.sendMessageAboutNewOrder(order);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramSenderService).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo(chatId);
        assertThat(sentMessage.getParseMode()).isEqualTo("HTML");
        assertThat(sentMessage.getText()).contains("Нове замовлення");
        assertThat(sentMessage.getText()).contains("NUM");
        assertThat(sentMessage.getText()).contains("Laptop (x1)");
        assertThat(sentMessage.getText()).contains("м. Kyiv");
    }

    @Test
    @DisplayName("Should handle TelegramApiException and log error")
    void sendMessageAboutNewOrder_shouldHandleException() throws TelegramApiException {

        when(telegramConfig.getOrderChatId()).thenReturn("123");
        OrderResponseDTO order = createSimpleOrder();

        doThrow(new TelegramApiException("API Error")).when(telegramSenderService).execute(any(SendMessage.class));

        telegramSenderService.sendMessageAboutNewOrder(order);

        verify(telegramSenderService).execute(any(SendMessage.class));
    }

    private OrderResponseDTO createSimpleOrder() {
        OrderItemDTO item = new OrderItemDTO( 1L, "Laptop", new BigDecimal("25000"),  new BigDecimal("20000"), 1, new BigDecimal("25000"));
        AddressDTO address = new AddressDTO("Kyiv", "Kyiv region", "1", "Main St", "10", "5");
        return new OrderResponseDTO(
                UUID.randomUUID(), "NUM", OffsetDateTime.now(), OrderStatus.NEW, PaymentType.CARD, PaymentStatus.SUCCESS,
                DeliveryType.UKR_POSHTA,
                address,
                "email","phone", "f", "s",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"),
                List.of(item)
        );
    }
}
