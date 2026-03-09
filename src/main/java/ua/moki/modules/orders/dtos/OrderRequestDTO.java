package ua.moki.modules.orders.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.PaymentType;

import java.util.List;

public record OrderRequestDTO(
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
        @NotNull(message = "Delivery type is required")
        DeliveryType deliveryType,
        @NotNull(message = "Payment type is required")
        PaymentType paymentType,
        @Valid
        @NotEmpty(message = "Order cannot be empty")
        List<CartItemDTO> cartItems,
        @Valid
        @NotNull(message = "Delivery address is required")
        AddressDTO addressDTO
) {
}
