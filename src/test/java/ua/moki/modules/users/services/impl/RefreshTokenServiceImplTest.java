package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.users.domains.RefreshToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.RefreshTokenRepository;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.security.Token;
import ua.moki.modules.users.security.jwt.RefreshTokenJweStringSerializer;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RefreshTokenServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenJweStringSerializer refreshTokenSerializer;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = new User();
        testUser.setPublicId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setSecondName("User");
        testUser.setEmail("refresh.integration@test.com");
        testUser.setPhoneNumber("+380990000001");
        testUser.setPassword("password");
        testUser.setRoleType(RoleType.CUSTOMER);
        testUser.setDeleted(false);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("saveRefreshToken successfully saves the token to the database")
    void saveRefreshToken_shouldPersistToken() {

        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        Token tokenObj = new Token(testUser.getPublicId().toString(), Collections.emptyList(), Instant.now(), expiresAt);
        String tokenString = "some.encrypted.token.string";

        refreshTokenService.saveRefreshToken(tokenString, tokenObj, testUser);

        RefreshToken savedToken = refreshTokenRepository.findByToken(tokenString).orElseThrow();
        assertThat(savedToken.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedToken.getExpiryDate()).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("consumeRefreshTokenAndGetUser successfully returns the user and deletes the used token")
    void consumeRefreshToken_shouldReturnUserAndRotateToken() {

        Token tokenObj = new Token(
                testUser.getPublicId().toString(),
                Collections.singletonList(RoleType.CUSTOMER.name()),
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );
        String validTokenString = refreshTokenSerializer.apply(tokenObj);

        refreshTokenService.saveRefreshToken(validTokenString, tokenObj, testUser);

        User resultUser = refreshTokenService.consumeRefreshTokenAndGetUser(validTokenString);

        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(testUser.getId());

        assertThat(refreshTokenRepository.findByToken(validTokenString)).isEmpty();
    }

    @Test
    @DisplayName("consumeRefreshTokenAndGetUser throws InvalidTokenException and deletes the token if it has expired in the DB")
    void consumeRefreshToken_shouldThrowException_whenTokenExpiredInDb() {

        Token tokenObj = new Token(
                testUser.getPublicId().toString(),
                Collections.emptyList(),
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );
        String tokenString = refreshTokenSerializer.apply(tokenObj);

        RefreshToken expiredEntity = new RefreshToken();
        expiredEntity.setToken(tokenString);
        expiredEntity.setUser(testUser);
        expiredEntity.setExpiryDate(Instant.now().minus(1, ChronoUnit.SECONDS));
        refreshTokenRepository.save(expiredEntity);

        assertThatThrownBy(() -> refreshTokenService.consumeRefreshTokenAndGetUser(tokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token expired");

        assertThat(refreshTokenRepository.findByToken(tokenString)).isEmpty();
    }

    @Test
    @DisplayName("consumeRefreshTokenAndGetUser throws InvalidTokenException if token is not found in the DB (Replay Attack)")
    void consumeRefreshToken_shouldThrowException_whenTokenMissingInDb() {

        Token tokenObj = new Token(testUser.getPublicId().toString(), Collections.emptyList(), Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
        String validTokenString = refreshTokenSerializer.apply(tokenObj);

        assertThatThrownBy(() -> refreshTokenService.consumeRefreshTokenAndGetUser(validTokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token not found or already used");
    }

    @Test
    @DisplayName("deleteAllForUser deletes all tokens for a specific user")
    void deleteAllForUser_shouldRemoveAllTokens() {

        createAndSaveToken(testUser, "token1");
        createAndSaveToken(testUser, "token2");

        User otherUser = new User();
        otherUser.setFirstName("Other");
        otherUser.setSecondName("User");
        otherUser.setPhoneNumber("+380990000002");
        otherUser.setPassword("otherpass");
        otherUser.setRoleType(RoleType.CUSTOMER);
        otherUser.setDeleted(false);

        otherUser.setPublicId(UUID.randomUUID());
        otherUser.setEmail("other@test.com");
        otherUser.setRoleType(RoleType.CUSTOMER);
        otherUser.setDeleted(false);
        userRepository.save(otherUser);
        createAndSaveToken(otherUser, "token_other");

        assertThat(refreshTokenRepository.count()).isEqualTo(3);

        refreshTokenService.deleteAllForUser(testUser.getId());

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken("token_other")).isPresent();
        assertThat(refreshTokenRepository.findByToken("token1")).isEmpty();
    }

    private void createAndSaveToken(User user, String tokenStr) {
        RefreshToken token = new RefreshToken();
        token.setToken(tokenStr);
        token.setUser(user);
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        refreshTokenRepository.save(token);
    }
}
