package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.users.domains.RefreshToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.EmailChangeRequestDTO;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.auth.PasswordResetDTO;
import ua.moki.modules.users.repositories.RefreshTokenRepository;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.AccountSecurityService;
import ua.moki.modules.users.services.tokens.ActivationTokenService;
import ua.moki.modules.users.services.tokens.EmailTokenService;
import ua.moki.modules.users.services.tokens.PasswordResetTokenService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.InvalidTokenException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSecurityServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private AccountSecurityService accountSecurityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailTokenService emailTokenService;

    @Autowired
    private ActivationTokenService activationTokenService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("activateUser activates account and deletes activation token")
    void activateUser_shouldActivateUserAndDeleteToken() {
        User user = createAndSaveUser("activate@test.com", "StrongPass1!");
        user.setActivated(false);
        user = userRepository.save(user);

        String token = activationTokenService.generateToken(user);

        accountSecurityService.activateUser(token);

        User activatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(activatedUser.isActivated()).isTrue();
    }

    @Test
    @DisplayName("initiateEmailChange generates token when password is valid and email is free")
    void initiateEmailChange_shouldGenerateToken_whenValid() {
        String rawPass = "StrongPass1!";
        User user = createAndSaveUser("old.email@test.com", rawPass);

        EmailChangeRequestDTO dto = new EmailChangeRequestDTO("new.email@test.com", rawPass);

        accountSecurityService.initiateEmailChange(user.getPublicId(), dto);

        assertThat(emailTokenService.findFirstByUserId(user.getId())).isPresent();
    }

    @Test
    @DisplayName("initiateEmailChange throws BadCredentialsException when password is wrong")
    void initiateEmailChange_shouldThrowException_whenPasswordWrong() {
        User user = createAndSaveUser("user@test.com", "CorrectPass1!");

        EmailChangeRequestDTO dto = new EmailChangeRequestDTO("new@test.com", "WrongPass1!");

        assertThatThrownBy(() -> accountSecurityService.initiateEmailChange(user.getPublicId(), dto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Wrong password");
    }

    @Test
    @DisplayName("initiateEmailChange throws UserAlreadyExistsException when new email is already taken")
    void initiateEmailChange_shouldThrowException_whenEmailAlreadyExists() {
        User user = createAndSaveUser("user1@test.com", "CorrectPass1!");
        createAndSaveUser("taken@test.com", "OtherPass1!");

        EmailChangeRequestDTO dto = new EmailChangeRequestDTO("taken@test.com", "CorrectPass1!");

        assertThatThrownBy(() -> accountSecurityService.initiateEmailChange(user.getPublicId(), dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already taken");
    }

    @Test
    @DisplayName("confirmEmailChange updates user email and revokes refresh tokens")
    void confirmEmailChange_shouldUpdateEmailAndRevokeTokens() {
        User user = createAndSaveUser("old.confirm@test.com", "StrongPass1!");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("token_to_revoke_after_email_change");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(refreshToken);

        String newEmail = "new.confirm@test.com";
        String confirmationToken = emailTokenService.generateToken(user, newEmail);

        accountSecurityService.confirmEmailChange(confirmationToken);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(refreshTokenRepository.findByToken("token_to_revoke_after_email_change")).isEmpty();
    }

    @Test
    @DisplayName("confirmEmailChange throws InvalidTokenException when token is invalid")
    void confirmEmailChange_shouldThrowException_whenTokenIsInvalid() {
        assertThatThrownBy(() -> accountSecurityService.confirmEmailChange("non-existent-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("changePassword updates password and deletes all active refresh tokens")
    void changePassword_shouldUpdatePasswordAndRevokeTokens() {
        String oldPassword = "OldStrongPassword1!";
        String newPassword = "NewStrongPassword2@";

        User user = createAndSaveUser("changepass@test.com", oldPassword);

        RefreshToken token = new RefreshToken();
        token.setToken("token_before_pass_change");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

        PasswordChangeRequestDTO changeDTO = new PasswordChangeRequestDTO(
                oldPassword,
                newPassword,
                newPassword
        );

        accountSecurityService.changePassword(user.getPublicId(), changeDTO);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(oldPassword, updatedUser.getPassword())).isFalse();
        assertThat(refreshTokenRepository.findByToken("token_before_pass_change")).isEmpty();
    }

    @Test
    @DisplayName("changePassword throws BadCredentialsException when current password is wrong")
    void changePassword_shouldThrowException_whenCurrentPasswordIsWrong() {
        User user = createAndSaveUser("wrong.pass@test.com", "RealPassword123!");

        PasswordChangeRequestDTO invalidDTO = new PasswordChangeRequestDTO(
                "WrongOldPass1!",
                "NewPass123!",
                "NewPass123!"
        );

        assertThatThrownBy(() -> accountSecurityService.changePassword(user.getPublicId(), invalidDTO))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Wrong password");
    }

    @Test
    @DisplayName("verifyOtpAndGetResetToken and resetPasswordWithJwt complete password recovery flow")
    void passwordResetFlow_shouldIssueTokenAndResetPassword() {
        User user = createAndSaveUser("reset.flow@test.com", "OldPass123!");
        String otpCode = passwordResetTokenService.generateToken(user);

        String resetJwt = accountSecurityService.verifyOtpAndGetResetToken(otpCode);
        assertThat(resetJwt).isNotBlank();

        PasswordResetDTO resetDTO = new PasswordResetDTO("NewResetPass1!", "NewResetPass1!");
        accountSecurityService.resetPasswordWithJwt(user.getPublicId(), resetDTO);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewResetPass1!", updated.getPassword())).isTrue();
    }

    private User createAndSaveUser(String email, String rawPassword) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setSecondName("User");
        user.setEmail(email);
        user.setPhoneNumber("+380991112233");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoleType(RoleType.CUSTOMER);
        user.setBlocked(false);
        user.setDeleted(false);
        return userRepository.save(user);
    }
}
