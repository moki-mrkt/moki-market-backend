package ua.moki.modules.orders.dtos;

import jakarta.validation.constraints.NotBlank;

public record AddressDTO (
        @NotBlank(message = "City is required")
        String city,
        String region,
        @NotBlank(message = "Department or Street is required")
        String department,
        String street,
        String houseNumber,
        String apartment
) {
}
