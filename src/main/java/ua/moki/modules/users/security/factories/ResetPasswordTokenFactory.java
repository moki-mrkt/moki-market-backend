package ua.moki.modules.users.security.factories;

import org.springframework.stereotype.Component;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.security.Token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Component
public class ResetPasswordTokenFactory implements Function<User, Token> {

    @Override
    public Token apply(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(10));

        return new Token(
                user.getPublicId().toString(),
                List.of("OP_RESET_PASSWORD"),
                now,
                expiresAt
        );
    }
}
