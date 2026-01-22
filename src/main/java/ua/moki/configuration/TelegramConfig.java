package ua.moki.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Getter
@Configuration
public class TelegramConfig {

    @Value("${bot.name}")
    private String botName;
    @Value("${bot.key}")
    private String token;
    @Value("${bot.chat.id}")
    private String chatId;

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        return new DefaultBotOptions();
    }
}
