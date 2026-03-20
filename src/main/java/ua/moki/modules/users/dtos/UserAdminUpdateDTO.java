package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserAdminUpdateDTO(
        @NotBlank(message = "Phone number should not be empty")
        @Size(max = 13)
        @Pattern(regexp = "\\+[0-9]+", message = "Phone number is not correct")
        String phoneNumber,
        LocalDate dateOfBirth,
        boolean activated,
        boolean accessToAccount,
        boolean subscribedToNews,
        Integer numberOfFailedAttempts
) {
}