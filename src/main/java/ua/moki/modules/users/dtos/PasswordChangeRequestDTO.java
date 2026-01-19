package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.moki.modules.users.utils.PasswordConfirmable;
import ua.moki.modules.users.utils.PasswordMatches;

@PasswordMatches
public record PasswordChangeRequestDTO(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "Confirm password should not be empty")
        @Size(min = 8,
                max = 255,
                message = "Confirm password must be greater than 10 or invalid password")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,20}$")
        String password,
        @NotBlank(message = "Confirm password should not be empty")
        String confirmPassword
) implements PasswordConfirmable {
}
