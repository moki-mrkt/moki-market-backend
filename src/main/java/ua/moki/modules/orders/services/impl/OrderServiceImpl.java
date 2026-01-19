package ua.moki.modules.orders.services.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.orders.domains.Order;
import ua.moki.modules.orders.domains.OrderItem;
import ua.moki.modules.orders.dtos.CartItemDTO;
import ua.moki.modules.orders.dtos.OrderRequestDTO;
import ua.moki.modules.orders.repositories.OrderRepository;
import ua.moki.modules.orders.utils.mappers.OrderMapper;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.services.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl {

    UserService userService;
    ProductService productService;
    OrderRepository orderRepository;
    OrderMapper orderMapper;

    @Transactional
    public void createOrder(OrderRequestDTO dto, Authentication authentication) {
        Order order = new Order();

        // 1. Логіка: Хто це?
        if (authentication != null && authentication.isAuthenticated()) {
            // Це зареєстрований юзер
            UUID userId = UUID.fromString(authentication.getName());
            User user = userService.getActiveUserEntityByPublicId(userId);
            order.setUser(user);
        }
        // Якщо authentication == null, поле user залишиться null (Гість)

        // 2. Заповнюємо дані (вони приходять з форми checkout)
        order.setEmail(dto.email());
        order.setPhoneNumber(dto.phoneNumber());
        order.setFirstName(dto.firstName());
        order.setSecondName(dto.secondName());
        order.setDeliveryType(dto.deliveryType());
        order.setPaymentType(dto.paymentType());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDTO itemDto : dto.cartItems()) {
            Product product = productService.findById(itemDto.productId());

            OrderItem item = new OrderItem();
            item.setBasePriceAtPurchase(product.getPrice());
            item.setDiscountPercentageAtPurchase(product.getDiscount());

            BigDecimal discountAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(product.getDiscount()))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

            item.setDiscountAmountPerUnit(discountAmount);
            item.setFinalPricePerUnit(product.getPrice().subtract(discountAmount));

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.quantity())));
        }

        order.setTotalAmount(total);

        orderRepository.save(order);

        return orderMapper.toDto(order);
    }
}
