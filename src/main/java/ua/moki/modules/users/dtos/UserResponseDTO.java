package ua.moki.modules.users.dtos;

import ua.moki.modules.orders.utils.enums.DeliveryType;

import java.time.LocalDate;

public record UserResponseDTO(
        String id,
        String firstName,
        String secondName,
        String email,
        String phoneNumber,
        String imageUrl,
        String roleType,
        LocalDate dateOfBirth,
        DeliveryInfoDTO deliveryInfo
) {
}
