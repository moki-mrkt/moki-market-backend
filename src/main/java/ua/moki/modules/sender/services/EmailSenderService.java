package ua.moki.modules.sender.services;

public interface EmailSenderService {

    void sendVerificationMessage(String userEmail, String verificationLink);
    void sendEmailChangeMessage(String userEmail, String token);
    void sendSecurityAlertEmailMessage(String userOldEmail, String userNewEmail);
    void sendForgotPasswordMessage(String userEmail, String otpCode);
    void sendSecurityAlertPasswordChangedEmailMessage(String userEmail);
}
