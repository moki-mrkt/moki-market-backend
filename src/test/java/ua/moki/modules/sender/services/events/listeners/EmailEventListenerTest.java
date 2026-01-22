package ua.moki.modules.sender.services.events.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.sender.services.EmailSenderService;
import ua.moki.modules.sender.services.events.EmailChangeInitiatedEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailEventListenerTest {

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailEventListener emailEventListener;

    @Test
    @DisplayName("Should call emailSenderService when event is handled")
    void handleEmailChange_Success() {

        EmailChangeInitiatedEvent event = new EmailChangeInitiatedEvent("test@moki.ua", "token-123");

        emailEventListener.handleEmailChange(event);

        verify(emailSenderService, times(1))
                .sendVerificationMessage("test@moki.ua", "token-123");
    }

    @Test
    @DisplayName("Should handle exception when emailSenderService fails")
    void handleEmailChange_ErrorHandling() {

        EmailChangeInitiatedEvent event = new EmailChangeInitiatedEvent("test@moki.ua", "token-123");
        doThrow(new RuntimeException("Mail server down"))
                .when(emailSenderService).sendVerificationMessage(anyString(), anyString());

        assertDoesNotThrow(() -> emailEventListener.handleEmailChange(event));
        verify(emailSenderService).sendVerificationMessage(anyString(), anyString());
    }
}
