package ua.moki.modules.users.security.factories;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Setter
@Component
public class DefaultAccessTokenFactory implements Function<Token, Token> {

    @Value("${jwt.access.ttl}")
    private Duration tokenTtl;

    @Override
    public Token apply(Token refreshToken) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtl);

        List<String> authorities = refreshToken.authorities().stream()
                .filter(auth -> !auth.equals("JWT_REFRESH") && !auth.equals("JWT_LOGOUT"))
                .toList();

        return new Token(
                refreshToken.subject(),
                authorities,
                now,
                expiresAt
        );
    }
}
