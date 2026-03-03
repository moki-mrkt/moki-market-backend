package ua.moki.modules.users.dtos;

import jakarta.validation.constraints.Size;
import ua.moki.modules.orders.utils.enums.DeliveryType;

public record DeliveryInfoDTO(
        @Size(max = 255)
        String region,
        @Size(max = 255)
        String city,
        @Size(max = 255)
        String postOffice,
        @Size(max = 255)
        String street,
        @Size(max = 255)
        String house,
        DeliveryType deliveryType
) {
}
