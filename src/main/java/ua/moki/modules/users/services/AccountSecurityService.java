package ua.moki.modules.users.services;

import ua.moki.modules.users.dtos.EmailChangeRequestDTO;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.auth.PasswordResetDTO;

import java.util.UUID;

public interface AccountSecurityService {

    void activateUser(String token);
    void initiateEmailChange(UUID userId, EmailChangeRequestDTO dto);
    void confirmEmailChange(String token);
    void changePassword(UUID userId, PasswordChangeRequestDTO dto);
    void initiateForgotPassword(String userEmail);
    String verifyOtpAndGetResetToken(String otpCode);
    void resetPasswordWithJwt(UUID userId, PasswordResetDTO passwordResetDTO);
}
