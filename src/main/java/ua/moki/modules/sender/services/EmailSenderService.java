package ua.moki.modules.sender.services;

public interface EmailSenderService {

    void sendVerificationMessage(String userEmail, String verificationLink);
}
