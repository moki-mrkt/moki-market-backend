package ua.moki.modules.sender.services.events;

public record VerifyEmailEvent(String email, String activationToken) {
}
