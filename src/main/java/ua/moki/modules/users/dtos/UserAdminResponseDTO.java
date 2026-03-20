package ua.moki.modules.users.dtos;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserAdminResponseDTO(
        String id,
        String firstName,
        String secondName,
        String email,
        String phoneNumber,
        String imageUrl,
        String roleType,
        LocalDate dateOfBirth,
        boolean activated,
        boolean blocked,
        boolean accessToAccount,
        boolean subscribedToNews,
        Integer numberOfFailedAttempts,
        DeliveryInfoDTO deliveryInfo,
        OffsetDateTime creationTime
) {
}
