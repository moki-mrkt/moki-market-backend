package ua.moki.modules.users.security.factories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ua.moki.modules.users.security.Token;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.Mockito.*;

public class DefaultRefreshTokenFactoryTest {

    private DefaultRefreshTokenFactory factory;
    private final Duration tokenTtl = Duration.ofDays(30);

    @BeforeEach
    void setUp() {
        factory = new DefaultRefreshTokenFactory();
        factory.setTokenTtl(tokenTtl);
    }

    @Test
    @DisplayName("apply creates a token with the correct subject, time, and adds the required permissions (JWT_REFRESH, JWT_LOGOUT)")
    void apply_shouldCreateTokenWithDefaultAuthorities() {

        String username = "user-uuid-123";
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(username);

        doReturn(List.of()).when(authentication).getAuthorities();

        Token refreshToken = factory.apply(authentication);

        assertThat(refreshToken.subject()).isEqualTo(username);

        assertThat(refreshToken.createdAt())
                .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));

        assertThat(refreshToken.expiresAt())
                .isCloseTo(Instant.now().plus(tokenTtl), within(1, ChronoUnit.SECONDS));

        assertThat(refreshToken.authorities())
                .hasSize(2)
                .containsExactlyInAnyOrder("JWT_REFRESH", "JWT_LOGOUT");
    }

    @Test
    @DisplayName("apply combines user rights with technical rights")
    void apply_shouldMergeUserAuthorities() {

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin-user");

        GrantedAuthority adminRole = new SimpleGrantedAuthority("ROLE_ADMIN");
        GrantedAuthority userRole = new SimpleGrantedAuthority("ROLE_USER");

        doReturn(List.of(adminRole, userRole)).when(authentication).getAuthorities();

        Token refreshToken = factory.apply(authentication);

        assertThat(refreshToken.authorities())
                .hasSize(4)
                .contains("ROLE_ADMIN", "ROLE_USER")
                .contains("JWT_REFRESH", "JWT_LOGOUT");
    }
}
