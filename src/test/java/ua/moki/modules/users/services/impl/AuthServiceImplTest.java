package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.auth.AuthResponseDTO;
import ua.moki.modules.users.dtos.auth.LoginRequestDTO;
import ua.moki.modules.users.dtos.auth.RefreshTokenRequestDTO;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.security.Token;
import ua.moki.modules.users.security.factories.DefaultAccessTokenFactory;
import ua.moki.modules.users.security.factories.DefaultRefreshTokenFactory;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringSerializer;
import ua.moki.modules.users.security.jwt.RefreshTokenJweStringSerializer;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private DefaultRefreshTokenFactory refreshTokenFactory;
    @Mock private DefaultAccessTokenFactory accessTokenFactory;
    @Mock private AccessTokenJwsStringSerializer accessTokenSerializer;
    @Mock private RefreshTokenJweStringSerializer refreshTokenSerializer;

    private final Duration accessTokenTtl = Duration.ofMinutes(15);

    @BeforeEach
    void setUp() {

        authService = new AuthServiceImpl(
                accessTokenTtl,
                userRepository,
                refreshTokenService,
                authenticationManager,
                refreshTokenFactory,
                accessTokenFactory,
                accessTokenSerializer,
                refreshTokenSerializer
        );
    }

    @Test
    @DisplayName("login successfully authenticates the user and returns a token pair")
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {

        LoginRequestDTO loginRequest = new LoginRequestDTO("test@mail.com", "password");
        User user = createTestUser();
        Authentication authentication = mock(Authentication.class);

        Token mockRefreshTokenObj = createMockToken();
        Token mockAccessTokenObj = createMockToken();
        String refreshTokenString = "refresh.token.string";
        String accessTokenString = "access.token.string";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmailAndDeletedFalse(loginRequest.email()))
                .thenReturn(Optional.of(user));

        when(refreshTokenFactory.apply(authentication)).thenReturn(mockRefreshTokenObj);
        when(accessTokenFactory.apply(authentication)).thenReturn(mockAccessTokenObj);
        when(refreshTokenSerializer.apply(mockRefreshTokenObj)).thenReturn(refreshTokenString);
        when(accessTokenSerializer.apply(mockAccessTokenObj)).thenReturn(accessTokenString);

        AuthResponseDTO response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(accessTokenString);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenString);
        assertThat(response.expiresIn()).isEqualTo(accessTokenTtl.getSeconds());
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(refreshTokenService).saveRefreshToken(eq(refreshTokenString), eq(mockRefreshTokenObj), eq(user));
    }

    @Test
    @DisplayName("login throws BadCredentialsException if the password is incorrect")
    void login_shouldThrowException_whenAuthenticationFails() {

        LoginRequestDTO loginRequest = new LoginRequestDTO("wrong@mail.com", "wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepository, refreshTokenFactory);
    }

    @Test
    @DisplayName("login throws EntityNotFoundException if the user is not found in the database after successful authentication")
    void login_shouldThrowException_whenUserNotFoundInDb() {

        LoginRequestDTO loginRequest = new LoginRequestDTO("ghost@mail.com", "pass");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmailAndDeletedFalse(loginRequest.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("refreshAccessToken successfully refreshes tokens")
    void refreshAccessToken_shouldReturnNewTokens_whenRefreshTokenIsValid() {

        String oldRefreshTokenString = "old.refresh.token";
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(oldRefreshTokenString);
        User user = createTestUser();

        Token mockRefreshTokenObj = createMockToken();
        Token mockAccessTokenObj = createMockToken();
        String newRefreshTokenString = "new.refresh.token";
        String newAccessTokenString = "new.access.token";

        when(refreshTokenService.consumeRefreshTokenAndGetUser(oldRefreshTokenString)).thenReturn(user);

        when(refreshTokenFactory.apply(any(Authentication.class))).thenReturn(mockRefreshTokenObj);
        when(accessTokenFactory.apply(any(Authentication.class))).thenReturn(mockAccessTokenObj);

        when(refreshTokenSerializer.apply(mockRefreshTokenObj)).thenReturn(newRefreshTokenString);
        when(accessTokenSerializer.apply(mockAccessTokenObj)).thenReturn(newAccessTokenString);

        AuthResponseDTO response = authService.refreshAccessToken(refreshRequest);

        assertThat(response.accessToken()).isEqualTo(newAccessTokenString);
        assertThat(response.refreshToken()).isEqualTo(newRefreshTokenString);

        verify(refreshTokenService).consumeRefreshTokenAndGetUser(oldRefreshTokenString);
        verify(refreshTokenService).saveRefreshToken(eq(newRefreshTokenString), eq(mockRefreshTokenObj), eq(user));
    }

    @Test
    @DisplayName("logout calls the service to delete the token")
    void logout_shouldCallDeleteRefreshToken() {

        String tokenToDelete = "token.to.delete";

        authService.logout(tokenToDelete);

        verify(refreshTokenService).deleteRefreshToken(tokenToDelete);
    }

    private User createTestUser() {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setEmail("test@mail.com");
        user.setRoleType(RoleType.CUSTOMER);
        return user;
    }

    private Token createMockToken() {
        return new Token(
                "subject",
                Collections.emptyList(),
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
    }
}
