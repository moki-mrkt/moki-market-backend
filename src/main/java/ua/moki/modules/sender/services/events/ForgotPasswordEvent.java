package ua.moki.modules.sender.services.events;

public record ForgotPasswordEvent(String userEmail, String otpCode) {
}
