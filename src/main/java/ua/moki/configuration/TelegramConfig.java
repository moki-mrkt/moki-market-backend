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
    @Value("${bot.order.chat.id}")
    private String orderChatId;
    @Value("${bot.photo.chat.id}")
    private String photoChatId;

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        return new DefaultBotOptions();
    }
}
