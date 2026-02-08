package ua.moki.modules.users.security.factories;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Setter
@Component
public class DefaultAccessTokenFactory implements Function<Authentication, Token> {

    @Value("${jwt.access.ttl}")
    private Duration tokenTtl;

    @Override
    public Token apply(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtl);

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new Token(
                authentication.getName(),
                authorities,
                now,
                expiresAt
        );
    }
}
