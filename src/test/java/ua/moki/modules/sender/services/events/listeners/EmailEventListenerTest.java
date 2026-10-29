package ua.moki.modules.sender.services.events.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.sender.services.EmailSenderService;
import ua.moki.modules.sender.services.events.EmailChangeInitiatedEvent;
import ua.moki.modules.sender.services.events.ForgotPasswordEvent;
import ua.moki.modules.sender.services.events.SecurityAlertEmailEvent;
import ua.moki.modules.sender.services.events.SecurityAlertPasswordChangedEvent;
import ua.moki.modules.sender.services.events.VerifyEmailEvent;
import ua.moki.modules.sender.services.listeners.EmailEventListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailEventListenerTest {

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailEventListener emailEventListener;

    @Test
    @DisplayName("Should call sendVerificationMessage when VerifyEmailEvent is handled")
    void handleVerificationEmail_Success() {
        VerifyEmailEvent event = new VerifyEmailEvent("test@moki.ua", "activation-123");

        emailEventListener.handleVerificationEmail(event);

        verify(emailSenderService, times(1))
                .sendVerificationMessage("test@moki.ua", "activation-123");
    }

    @Test
    @DisplayName("Should call sendEmailChangeMessage when EmailChangeInitiatedEvent is handled")
    void handleEmailChange_Success() {
        EmailChangeInitiatedEvent event = new EmailChangeInitiatedEvent("test@moki.ua", "token-123");

        emailEventListener.handleEmailChange(event);

        verify(emailSenderService, times(1))
                .sendEmailChangeMessage("test@moki.ua", "token-123");
    }

    @Test
    @DisplayName("Should handle exception when sendEmailChangeMessage fails")
    void handleEmailChange_ErrorHandling() {
        EmailChangeInitiatedEvent event = new EmailChangeInitiatedEvent("test@moki.ua", "token-123");
        doThrow(new RuntimeException("Mail server down"))
                .when(emailSenderService).sendEmailChangeMessage(anyString(), anyString());

        assertDoesNotThrow(() -> emailEventListener.handleEmailChange(event));
        verify(emailSenderService).sendEmailChangeMessage("test@moki.ua", "token-123");
    }

    @Test
    @DisplayName("Should call sendSecurityAlertEmailMessage when SecurityAlertEmailEvent is handled")
    void handleSecurityAlertEmailEvent_Success() {
        SecurityAlertEmailEvent event = new SecurityAlertEmailEvent("old@moki.ua", "new@moki.ua");

        emailEventListener.handleSecurityAlertEmailEvent(event);

        verify(emailSenderService, times(1))
                .sendSecurityAlertEmailMessage("old@moki.ua", "new@moki.ua");
    }

    @Test
    @DisplayName("Should call sendForgotPasswordMessage when ForgotPasswordEvent is handled")
    void handleForgotPasswordEmailEvent_Success() {
        ForgotPasswordEvent event = new ForgotPasswordEvent("test@moki.ua", "123456");

        emailEventListener.handleForgotPasswordEmailEvent(event);

        verify(emailSenderService, times(1))
                .sendForgotPasswordMessage("test@moki.ua", "123456");
    }

    @Test
    @DisplayName("Should call sendSecurityAlertPasswordChangedEmailMessage when SecurityAlertPasswordChangedEvent is handled")
    void handleSSecurityAlertPasswordChangedEvent_Success() {
        SecurityAlertPasswordChangedEvent event = new SecurityAlertPasswordChangedEvent("test@moki.ua");

        emailEventListener.handleSSecurityAlertPasswordChangedEvent(event);

        verify(emailSenderService, times(1))
                .sendSecurityAlertPasswordChangedEmailMessage("test@moki.ua");
    }
}
