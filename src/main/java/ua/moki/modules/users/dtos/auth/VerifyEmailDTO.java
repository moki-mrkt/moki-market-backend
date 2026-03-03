package ua.moki.modules.users.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailDTO(
        @NotBlank @Email(regexp = ".+@.+\\..+", message = "Email is not correct")
        String email
) {
}
