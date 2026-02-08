package ua.moki.modules.users.security.factories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ua.moki.modules.users.security.Token;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.Mockito.*;

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

        List<String> listOfStringsOfAuthorities = List.of("ROLE_USER", "ROLE_MANAGER");

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_MANAGER"));

        String userId = "user-uuid-123";
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(userId);

        doReturn(authorities).when(authentication).getAuthorities();

        Token accessToken = factory.apply(authentication);

        assertThat(accessToken.subject()).isEqualTo(userId);

        assertThat(accessToken.createdAt())
                .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));

        assertThat(accessToken.expiresAt())
                .isCloseTo(Instant.now().plus(tokenTtl), within(1, ChronoUnit.SECONDS));

        assertThat(accessToken.authorities()).containsExactlyElementsOf(listOfStringsOfAuthorities);
    }

    @Test
    @DisplayName("apply filters technical rights (JWT REFRESH, JWT LOGOUT)")
    void apply_shouldFilterRestrictedAuthorities() {


        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_CUSTOMER"));


        String userId = "user-uuid-123";
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(userId);

        doReturn(authorities).when(authentication).getAuthorities();


        Token accessToken = factory.apply(authentication);

        assertThat(accessToken.authorities())
                .hasSize(2)
                .contains("ROLE_ADMIN", "ROLE_CUSTOMER")
                .doesNotContain("JWT_REFRESH", "JWT_LOGOUT");
    }

    @Test
    @DisplayName("apply correctly processes the list without technical rights")
    void apply_shouldKeepAuthorities_whenNoRestrictedPresent() {

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        String userId = "user-uuid-123";
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(userId);

        doReturn(authorities).when(authentication).getAuthorities();

        Token accessToken = factory.apply(authentication);

        assertThat(accessToken.authorities()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("apply correctly handles an empty list of permissions")
    void apply_shouldHandleEmptyAuthorities() {

        String userId = "user-uuid-123";
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(userId);

        doReturn(List.of()).when(authentication).getAuthorities();

        Token accessToken = factory.apply(authentication);

        assertThat(accessToken.authorities()).isEmpty();
    }
}
