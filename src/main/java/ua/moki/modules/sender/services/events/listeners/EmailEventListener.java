package ua.moki.modules.sender.services.events.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ua.moki.modules.sender.services.EmailSenderService;
import ua.moki.modules.sender.services.events.EmailChangeInitiatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

   private final EmailSenderService emailSenderService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailChange(EmailChangeInitiatedEvent event) {
        log.info("Sending email verification to {}", event.email());

        try {
            emailSenderService.sendVerificationMessage(event.email(), event.token());
        } catch (Exception e) {
            log.error("Failed to send email verification", e);
        }
    }
}
