package ua.moki.modules.orders.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record OrderUpdateDTO(
        @NotBlank
        @Email(regexp = ".+@.+\\..+",
                message = "Email is not correct")
        String email,
        @NotBlank(message = "Phone number should not be empty")
        @Size(max = 13)
        @Pattern(regexp = "\\+[0-9]+", message = "Phone number is not correct")
        String phoneNumber,
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
        @Valid
        @NotNull(message = "Delivery address is required")
        AddressDTO addressDTO
) {
}
