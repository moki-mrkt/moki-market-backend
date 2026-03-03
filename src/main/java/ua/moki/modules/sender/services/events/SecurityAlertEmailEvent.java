package ua.moki.modules.sender.services.events;

public record SecurityAlertEmailEvent(String userOldEmail, String userNewEmail) {
}
