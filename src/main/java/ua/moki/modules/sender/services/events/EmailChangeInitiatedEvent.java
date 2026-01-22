package ua.moki.modules.sender.services.events;

public record EmailChangeInitiatedEvent(String email, String token) {
}
