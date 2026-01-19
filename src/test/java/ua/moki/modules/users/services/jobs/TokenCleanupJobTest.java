package ua.moki.modules.users.services.jobs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.modules.users.domains.RefreshToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.RefreshTokenRepository;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // Використовуємо твою конфігурацію Testcontainers
@Transactional
public class TokenCleanupJobTest {

    @Autowired
    private TokenCleanupJob tokenCleanupJob;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("deleteExpiredTokens deletes ONLY expired tokens")
    void deleteExpiredTokens_shouldRemoveOnlyExpiredTokens() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setSecondName("User");
        user.setEmail("cleanup.test@mail.com");
        user.setPhoneNumber("+380990000000");
        user.setPassword("password");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        userRepository.save(user);

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expired-token-123");
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        refreshTokenRepository.save(expiredToken);

        RefreshToken validToken = new RefreshToken();
        validToken.setToken("valid-token-456");
        validToken.setUser(user);
        validToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        refreshTokenRepository.save(validToken);

        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        tokenCleanupJob.deleteExpiredTokens();

        assertThat(refreshTokenRepository.findByToken("expired-token-123")).isEmpty();

        assertThat(refreshTokenRepository.findByToken("valid-token-456")).isPresent();

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }
}
