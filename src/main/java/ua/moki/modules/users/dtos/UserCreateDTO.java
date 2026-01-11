package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.moki.modules.users.utils.PasswordConfirmable;
import ua.moki.modules.users.utils.PasswordMatches;

@PasswordMatches
public record UserCreateDTO(
        @NotBlank(message = "First name should not be empty")
        @Pattern(regexp = "^[A-ZА-ЩЬЮЯЄІЇҐЁЭЫЪ][a-zа-щьюяєіїґA-ZА-ЩЬЮЯЄІЇҐёэыъA-ZА-ЯЁЭЫЪ'ʼ’\\s-]+$",
                message = "First name is not correct")
        @Size(min = 2, max = 64, message = "First name is too short or too long")
        String firstName,
        @NotBlank(message = "Second name should not be empty")
        @Pattern(regexp = "^[A-ZА-ЩЬЮЯЄІЇҐЁЭЫЪ][a-zа-щьюяєіїґA-ZА-ЩЬЮЯЄІЇҐёэыъA-ZА-ЯЁЭЫЪ'ʼ’\\s-]+$",
                message = "Second name is not correct")
        @Size(min = 2, max = 64, message = "Second name is too short or too long")
        String secondName,
        @NotBlank
        @Email(regexp = ".+@.+\\..+",
                message = "Email is not correct")
        String email,
        @NotBlank(message = "Phone number should not be empty")
        @Size(max = 13)
        @Pattern(regexp = "\\+[0-9]+", message = "Phone number is not correct")
        String phoneNumber,
        @NotBlank(message = "Password should not be empty")
        @Size(min = 10,
                max = 255,
                message = "Password must be greater than 10 or invalid password")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,20}$")
        String password,
        @NotBlank(message = "Confirm password should not be empty")
        @Size(min = 10,
                max = 255,
                message = "Confirm password must be greater than 10 or invalid password")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,20}$")
        String confirmPassword
) implements PasswordConfirmable {

}
