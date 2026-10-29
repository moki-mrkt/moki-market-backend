package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.EmailChangeToken;
import ua.moki.modules.users.repositories.EmailChangeTokenRepository;
import ua.moki.util.exceptions.InvalidTokenException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailTokenServiceImplTest {

    @Mock
    private EmailChangeTokenRepository emailChangeTokenRepository;

    private EmailTokenServiceImpl emailTokenService;

    @BeforeEach
    void setUp() {
        emailTokenService = new EmailTokenServiceImpl(emailChangeTokenRepository);
    }

    @Test
    @DisplayName("generateToken deletes old tokens for user and saves a new EmailChangeToken")
    void generateToken_shouldDeleteOldTokensAndSaveNewOne() {
        User user = new User();
        user.setId(42L);
        String newEmail = "new@moki.ua";

        String rawToken = emailTokenService.generateToken(user, newEmail);

        assertThat(rawToken).isNotBlank();
        verify(emailChangeTokenRepository).deleteAllByUserId(42L);

        ArgumentCaptor<EmailChangeToken> captor = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(emailChangeTokenRepository).save(captor.capture());

        EmailChangeToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getNewEmail()).isEqualTo(newEmail);
        assertThat(saved.getToken()).isEqualTo(rawToken);
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("findByToken returns EmailChangeToken when found")
    void findByToken_shouldReturnToken_whenExists() {
        EmailChangeToken token = new EmailChangeToken();
        token.setToken("tok-123");
        token.setNewEmail("new@moki.ua");

        when(emailChangeTokenRepository.findByToken("tok-123")).thenReturn(Optional.of(token));

        EmailChangeToken found = emailTokenService.findByToken("tok-123");
        assertThat(found).isSameAs(token);
    }

    @Test
    @DisplayName("findByToken throws InvalidTokenException when token does not exist")
    void findByToken_shouldThrowInvalidTokenException_whenNotFound() {
        when(emailChangeTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailTokenService.findByToken("missing"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token");
    }
}
