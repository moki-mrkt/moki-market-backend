package ua.moki.modules.sender.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.moki.configuration.TelegramConfig;
import ua.moki.modules.orders.dtos.AddressDTO;
import ua.moki.modules.orders.dtos.OrderItemDTO;
import ua.moki.modules.orders.dtos.OrderResponseDTO;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.PaymentType;
import ua.moki.modules.sender.services.TelegramSenderService;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramSenderServiceImpl extends DefaultAbsSender implements TelegramSenderService {

    private final TelegramConfig telegramConfig;

    @Autowired
    protected TelegramSenderServiceImpl(DefaultBotOptions options,
                                        @Value("${bot.key}") String botToken,
                                        TelegramConfig telegramConfig) {
        super(options, botToken);
        this.telegramConfig = telegramConfig;
    }

    @Override
    public void sendMessageAboutNewOrder(OrderResponseDTO responseDTO) {
        SendMessage telegramMessage = new SendMessage();
        telegramMessage.setChatId(telegramConfig.getChatId());
        telegramMessage.setParseMode("HTML");

        String formattedDate = responseDTO.createAt().toString().replace("T", " ").substring(0, 16);

        telegramMessage.setText("""
                <b>Нове замовлення</b>
                
                <b>Номер</b>: %s
                <b>Дата</b>: %s
                <b>Клієнт</b>: %s %s
                <b>Телефон</b>: %s
                <b>Email</b>: %s
                
                <b>Відправка</b>: %s
                <b>Оплата</b>: %s
                <b>Сума</b>: %s
                <b>Адреса</b>: %s
                
                <b>Товари</b>:
                 %s
                """.formatted(
                responseDTO.orderNumber(),
                formattedDate,
                responseDTO.firstName(),
                responseDTO.secondName(),
                responseDTO.phoneNumber(),
                responseDTO.email(),
                formatDeliveryType(responseDTO.deliveryType()),
                formatPaymentType(responseDTO.paymentType()),
                responseDTO.total(),
                formatAddressDTO(responseDTO.addressDTO()),
                formatOrderItems(responseDTO.items())
        ));

        try {
            execute(telegramMessage);
            log.info("Telegram message about new order [%s] sent to bot".formatted(responseDTO.orderNumber()));
        } catch (TelegramApiException e) {
            log.error("Failed to send telegram message", e);
        }
    }

    private String formatOrderItems(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return "—";
        }

        return items.stream()

                .map(item -> String.format("▫️ %s (x%d) — %s грн",
                        item.itemName(),
                        item.quantity(),
                        item.finalPricePerUnit()))
                .collect(Collectors.joining("\n"));
    }

    private String formatAddressDTO(AddressDTO dto) {

        StringJoiner joiner = new StringJoiner(", ");


        addIfPresent(joiner, "м. ", dto.city());
        addIfPresent(joiner, "", dto.region());
        addIfPresent(joiner, "від. №", dto.department());
        addIfPresent(joiner, "вул. ", dto.street());
        addIfPresent(joiner, "буд. ", dto.houseNumber());
        addIfPresent(joiner, "кв. ", dto.apartment());

        return joiner.toString();
    }

    private void addIfPresent(StringJoiner joiner, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(prefix + value);
        }
    }

    private String formatDeliveryType(DeliveryType deliveryType) {
        return switch (deliveryType) {
            case DeliveryType.NOVA_POSHTA -> "Нова пошта";
            case DeliveryType.UKR_POSHTA -> "Укр пошта";
        };
    }

    private String formatPaymentType(PaymentType paymentType) {
        return switch (paymentType) {
            case PaymentType.CARD -> "На рахунок";
            case PaymentType.CASH -> "Готівка";
        };
    }
}
