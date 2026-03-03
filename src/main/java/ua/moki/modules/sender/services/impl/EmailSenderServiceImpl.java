package ua.moki.modules.sender.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import ua.moki.modules.sender.services.EmailSenderService;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSenderServiceImpl implements EmailSenderService {

    @Value("${spring.mail.username}")
    private String email;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendVerificationMessage(String userEmail, String verificationLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(userEmail);
            helper.setFrom(String.valueOf(new InternetAddress(email, "Moki Market")));
            helper.setSubject("Посилання для активації");

                Context context = new Context(Locale.getDefault());
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("token", verificationLink);
                context.setVariables(paramMap);

                String content = templateEngine.process("verifyEmail", context);

                helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email", e);
        }
    }

    @Override
    public void sendEmailChangeMessage(String userEmail, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(userEmail);
            helper.setFrom(String.valueOf(new InternetAddress(email, "Moki Market")));
            helper.setSubject("Підтвердження зміни пошти");

            Context context = new Context(Locale.getDefault());
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("token", token);
            context.setVariables(paramMap);

            String content = templateEngine.process("changeEmail", context);

            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email", e);
        }
    }

    @Override
    public void sendSecurityAlertEmailMessage(String userOldEmail, String userNewEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(userOldEmail);
            helper.setFrom(String.valueOf(new InternetAddress(email, "Moki Market")));
            helper.setSubject("Попередження про зміну пошти");

            Context context = new Context(Locale.getDefault());
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("newEmail", userNewEmail);
            context.setVariables(paramMap);


            String content = templateEngine.process("securityAlertChangeEmail", context);

            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email", e);
        }
    }

    @Override
    public void sendForgotPasswordMessage(String userEmail, String otpCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(userEmail);
            helper.setFrom(String.valueOf(new InternetAddress(email, "Moki Market")));
            helper.setSubject("Зміна пароля");

            Context context = new Context(Locale.getDefault());
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("otpCode", otpCode);
            context.setVariables(paramMap);


            String content = templateEngine.process("forgotPassword", context);

            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email", e);
        }
    }

    @Override
    public void sendSecurityAlertPasswordChangedEmailMessage(String userEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(userEmail);
            helper.setFrom(String.valueOf(new InternetAddress(email, "Moki Market")));
            helper.setSubject("Попередження про зміну пароля");

            Context context = new Context(Locale.getDefault());
            Map<String, Object> paramMap = new HashMap<>();
            context.setVariables(paramMap);

            String content = templateEngine.process("securityAlertPasswordChanged", context);

            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email", e);
        }
    }
}
