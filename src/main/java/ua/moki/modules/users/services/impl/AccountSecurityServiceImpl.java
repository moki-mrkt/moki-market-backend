package ua.moki.modules.users.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.sender.services.events.EmailChangeInitiatedEvent;
import ua.moki.modules.sender.services.events.ForgotPasswordEvent;
import ua.moki.modules.sender.services.events.SecurityAlertEmailEvent;
import ua.moki.modules.sender.services.events.SecurityAlertPasswordChangedEvent;
import ua.moki.modules.users.domains.tokens.ActivationToken;
import ua.moki.modules.users.domains.tokens.EmailChangeToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.PasswordResetToken;
import ua.moki.modules.users.dtos.EmailChangeRequestDTO;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.auth.PasswordResetDTO;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.security.Token;
import ua.moki.modules.users.security.factories.ResetPasswordTokenFactory;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringSerializer;
import ua.moki.modules.users.services.*;
import ua.moki.modules.users.services.tokens.ActivationTokenService;
import ua.moki.modules.users.services.tokens.EmailTokenService;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.modules.users.services.tokens.PasswordResetTokenService;
import ua.moki.util.OtpGenerator;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.InvalidTokenException;
import ua.moki.util.exceptions.TooManyRequestsException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountSecurityServiceImpl  implements AccountSecurityService {

    UserRepository userRepository;

    ResetPasswordTokenFactory resetTokenFactory;
    AccessTokenJwsStringSerializer accessTokenSerializer;

    PasswordEncoder passwordEncoder;
    EmailTokenService emailTokenService;
    ActivationTokenService activationTokenService;
    PasswordResetTokenService passwordResetTokenService;
    RefreshTokenService refreshTokenService;

    ApplicationEventPublisher eventPublisher;

    private User getActiveUserEntityByPublicId(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedFalse(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id [%s]".formatted(publicId)));
    }

    @Override
    @Transactional
    public void activateUser(String token) {

        ActivationToken activationToken = activationTokenService.findByToken(token);

        if (activationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            activationTokenService.deleteToken(activationToken);
            throw new InvalidTokenException("Activation token has expired");
        }

        User user = activationToken.getUser();
        user.setActivated(true);
        userRepository.save(user);

        activationTokenService.deleteToken(activationToken);
    }



    @Override
    @Transactional
    public void initiateEmailChange(UUID userId, EmailChangeRequestDTO dto) {

        User user = getActiveUserEntityByPublicId(userId);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (userRepository.existsByEmail(dto.newEmail())) {
            throw new UserAlreadyExistsException("Email already taken");
        }

        String token = emailTokenService.generateToken(user, dto.newEmail());

        // TODO write unit tests
        eventPublisher.publishEvent(
                new EmailChangeInitiatedEvent(dto.newEmail(), token)
        );

        eventPublisher.publishEvent(
                new SecurityAlertEmailEvent(user.getEmail(), dto.newEmail())
        );
    }

    @Override
    @Transactional
    public void confirmEmailChange(String token) {

        EmailChangeToken emailChangeToken = emailTokenService.findByToken(token);

        if (emailChangeToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            emailTokenService.deleteToken(emailChangeToken);
            throw new InvalidTokenException("Token has expired");
        }

        User user = emailChangeToken.getUser();

        user.setEmail(emailChangeToken.getNewEmail());
        userRepository.save(user);

        emailTokenService.deleteToken(emailChangeToken);

        refreshTokenService.deleteAllForUser(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequestDTO dto) {

        User user = getActiveUserEntityByPublicId(userId);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid current password");
        }

        if (passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as old password");
        }

        user.setPassword(passwordEncoder.encode(dto.password()));
        userRepository.save(user);

        refreshTokenService.deleteAllForUser(user.getId());
    }

    @Override
    @Transactional
    public void initiateForgotPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmailAndDeletedFalse(email);

        if (optionalUser.isEmpty() || optionalUser.get().isBlocked()) {
            log.warn("Password reset initiated for non-existent or blocked email: {}", email);
            return;
        }

        User user = optionalUser.get();

        passwordResetTokenService.findFirstByUserId(user.getId()).ifPresent(existingToken -> {
            if (existingToken.getExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(14))) {
                log.warn("Rate limit triggered for password reset: user {}", user.getPublicId());
                throw new TooManyRequestsException("Запит на відновлення пароля можна відправляти не частіше 1 разу на хвилину.");
            }
        });

        String otpCode = passwordResetTokenService.generateToken(user);

        eventPublisher.publishEvent(
                new ForgotPasswordEvent(user.getEmail(), otpCode)
        );

        log.info("Password reset OTP generated for user: {}", user.getPublicId());
    }

    @Override
    @Transactional
    public String verifyOtpAndGetResetToken(String otpCode) {

        PasswordResetToken passwordResetToken = passwordResetTokenService.findByToken(otpCode);

        if (passwordResetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            passwordResetTokenService.deleteToken(passwordResetToken);
            log.warn("Expired OTP code used for user: {}", passwordResetToken.getUser().getPublicId());
            throw new InvalidTokenException("OTP code has expired");
        }

        User user = passwordResetToken.getUser();

        if (user.isBlocked()) {
            log.warn("Blocked user {} attempted to verify OTP", user.getPublicId());
            throw new InvalidTokenException("Account is blocked");
        }

        passwordResetTokenService.deleteToken(passwordResetToken);

        Token tokenObj = resetTokenFactory.apply(user);
        log.info("Temporary reset JWT issued for user: {}", user.getPublicId());

        return accessTokenSerializer.apply(tokenObj);
    }

    @Override
    @Transactional
    public void resetPasswordWithJwt(UUID userId, PasswordResetDTO passwordResetDTO) {

        User user = getActiveUserEntityByPublicId(userId);

        if (user.isBlocked()) {
            log.warn("Blocked user {} attempted to reset password using JWT", userId);
            throw new IllegalArgumentException("Account is blocked");
        }

        user.setPassword(passwordEncoder.encode(passwordResetDTO.password()));
        user.setNumberOfFailedAttempts(0);
        userRepository.save(user);

        refreshTokenService.deleteAllForUser(user.getId());

        eventPublisher.publishEvent(
                new SecurityAlertPasswordChangedEvent(user.getEmail())
        );
        log.info("Password successfully reset for user: {}", userId);
    }
}
