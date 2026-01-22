package ua.moki.modules.sender.services.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailSenderServiceImpl emailSenderService;

    @Test
    void sendVerificationMessage_shouldCallMailSenderWithCorrectData() {

        String email = "test@user.com";
        String link = "http://moki.com/verify?token=123";

        ReflectionTestUtils.setField(emailSenderService, "email", "info@moki.market");

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("sample"), any(Context.class))).thenReturn("<html>TestContent</html>");

        emailSenderService.sendVerificationMessage(email, link);

        verify(mailSender, times(1)).send(any(MimeMessage.class));

        verify(templateEngine).process(eq("sample"), argThat(context ->
                context.getVariable("token").equals(link)
        ));
    }
}
