package ua.moki.modules.sender.services.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ua.moki.modules.sender.services.EmailSenderService;
import ua.moki.modules.sender.services.events.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

   private final EmailSenderService emailSenderService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerifyEmailEvent event) {
        log.info("Sending email verification to {}", event.email());

        try {
            emailSenderService.sendVerificationMessage(event.email(), event.activationToken());
        } catch (Exception e) {
            log.error("Failed to send email verification", e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailChange(EmailChangeInitiatedEvent event) {
        log.info("Sending changing email  to {}", event.email());

        try {
            emailSenderService.sendEmailChangeMessage(event.email(), event.token());
        } catch (Exception e) {
            log.error("Failed to send email verification", e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityAlertEmailEvent(SecurityAlertEmailEvent event) {
        log.info("Sending security alert email  to {}", event.userOldEmail());

        try {
            emailSenderService.sendSecurityAlertEmailMessage(event.userOldEmail(), event.userNewEmail());
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleForgotPasswordEmailEvent(ForgotPasswordEvent event) {
        log.info("Sending forgot password email  to {}", event.userEmail());

        try {
            emailSenderService.sendForgotPasswordMessage(event.userEmail(), event.otpCode());
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSSecurityAlertPasswordChangedEvent(SecurityAlertPasswordChangedEvent event) {
        log.info("Sending security alert password changed email  to {}", event.email());

        try {
            emailSenderService.sendSecurityAlertPasswordChangedEmailMessage(event.email());
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }
}
