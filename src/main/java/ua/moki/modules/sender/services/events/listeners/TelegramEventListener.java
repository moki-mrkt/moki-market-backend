package ua.moki.modules.sender.services.events.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ua.moki.modules.sender.services.TelegramSenderService;
import ua.moki.modules.sender.services.events.TelegramNewOrderEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramEventListener {

    private final TelegramSenderService telegramSender;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailChange(TelegramNewOrderEvent event) {
        log.info("Sending telegram message about new order {}", event.responseDTO().orderNumber());

        try {
            telegramSender.sendMessageAboutNewOrder(event.responseDTO());
        } catch (Exception e) {
            log.error("Failed to send telegram message", e);
        }
    }
}
