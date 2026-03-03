package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserUpdateDTO(
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
        @NotBlank(message = "Phone number should not be empty")
        @Size(max = 13)
        @Pattern(regexp = "\\+[0-9]+", message = "Phone number is not correct")
        String phoneNumber,
        LocalDate dateOfBirth,
        DeliveryInfoDTO deliveryInfo
) {
}
