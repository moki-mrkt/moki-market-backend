package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequestDTO(
    @NotBlank(message = "New email is required")
    @Email(regexp = ".+@.+\\..+", message = "Email is not correct")
    String newEmail,
    @NotBlank(message = "Current password is required")
    String currentPassword
) {
}
