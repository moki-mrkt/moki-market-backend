package ua.moki.modules.users.security.factories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.moki.modules.users.security.Token;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.within;

public class DefaultAccessTokenFactoryTest {

    private DefaultAccessTokenFactory factory;
    private final Duration tokenTtl = Duration.ofMinutes(15);

    @BeforeEach
    void setUp() {
        factory = new DefaultAccessTokenFactory();
        factory.setTokenTtl(tokenTtl);
    }

    @Test
    @DisplayName("apply creates a new token with the correct TTL and copies the subject")
    void apply_shouldCreateTokenWithCorrectTtlAndSubject() {

        UUID userId = UUID.randomUUID();
        List<String> authorities = List.of("ROLE_USER", "ROLE_MANAGER");

        Token refreshToken = new Token(
                userId.toString(),
                authorities,
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        Token accessToken = factory.apply(refreshToken);

        assertThat(accessToken.subject()).isEqualTo(userId.toString());

        assertThat(accessToken.createdAt())
                .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));

        assertThat(accessToken.expiresAt())
                .isCloseTo(Instant.now().plus(tokenTtl), within(1, ChronoUnit.SECONDS));

        assertThat(accessToken.authorities()).containsExactlyElementsOf(authorities);
    }

    @Test
    @DisplayName("apply filters technical rights (JWT REFRESH, JWT LOGOUT)")
    void apply_shouldFilterRestrictedAuthorities() {

        String userId = UUID.randomUUID().toString();

        List<String> mixedAuthorities = List.of(
                "ROLE_ADMIN",
                "JWT_REFRESH",
                "ROLE_CUSTOMER",
                "JWT_LOGOUT"
        );

        Token refreshToken = new Token(
                userId,
                mixedAuthorities,
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        Token accessToken = factory.apply(refreshToken);

        assertThat(accessToken.authorities())
                .hasSize(2)
                .contains("ROLE_ADMIN", "ROLE_CUSTOMER")
                .doesNotContain("JWT_REFRESH", "JWT_LOGOUT");
    }

    @Test
    @DisplayName("apply correctly processes the list without technical rights")
    void apply_shouldKeepAuthorities_whenNoRestrictedPresent() {

        List<String> authorities = List.of("ROLE_USER");
        Token refreshToken = new Token(
                UUID.randomUUID().toString(),
                authorities,
                Instant.now(),
                Instant.now()
        );

        Token accessToken = factory.apply(refreshToken);

        assertThat(accessToken.authorities()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("apply correctly handles an empty list of permissions")
    void apply_shouldHandleEmptyAuthorities() {

        Token refreshToken = new Token(
                UUID.randomUUID().toString(),
                List.of(),
                Instant.now(),
                Instant.now()
        );

        Token accessToken = factory.apply(refreshToken);

        assertThat(accessToken.authorities()).isEmpty();
    }
}
