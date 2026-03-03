package ua.moki.modules.users.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import ua.moki.modules.users.utils.PasswordConfirmable;
import ua.moki.modules.users.utils.PasswordMatches;

@PasswordMatches
public record PasswordResetDTO(
        @NotBlank @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,20}$")
        String password,
        @NotBlank
        String confirmPassword
) implements PasswordConfirmable {
}
