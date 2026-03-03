package ua.moki.modules.users.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyOtpDTO(
        @NotBlank @Email String email,
        @NotBlank(message = "OTP code is required")
        String otpCode
) {
}
