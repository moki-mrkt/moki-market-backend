package ua.moki.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramConfig {

    @Value("${bot.name}")
    private String botName;
    @Value("${bot.key}")
    private String token;
    @Value("${bot.chatId}")
    private String chatId;
}
