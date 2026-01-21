package ua.moki.modules.orders.dtos;

import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.enums.PaymentStatus;
import ua.moki.modules.orders.utils.enums.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO (
    UUID id,
    String orderNumber,
    OffsetDateTime createAt,
    OrderStatus orderStatus,
    PaymentType paymentType,
    PaymentStatus paymentStatus,
    DeliveryType deliveryType,
    AddressDTO addressDTO,
    String email,
    String phoneNumber,
    String firstName,
    String secondName,
    BigDecimal itemsTotal,
    BigDecimal discountTotal,
    BigDecimal total,
    List<OrderItemDTO> items
) {
}
