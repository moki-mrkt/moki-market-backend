package ua.moki.modules.orders.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.orders.domains.Order;
import ua.moki.modules.orders.domains.OrderItem;
import ua.moki.modules.orders.dtos.CartItemDTO;
import ua.moki.modules.orders.dtos.OrderRequestDTO;
import ua.moki.modules.orders.dtos.OrderResponseDTO;
import ua.moki.modules.orders.dtos.OrderUpdateDTO;
import ua.moki.modules.orders.repositories.OrderRepository;
import ua.moki.modules.orders.services.OrderService;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.mappers.OrderMapper;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.OutOfStockException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    UserService userService;
    ProductService productService;
    OrderRepository orderRepository;
    OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {

        Order order = orderMapper.toEntity(dto);

        enrichOrderWithMeta(order, SecurityContextHolder.getContext().getAuthentication());

        processOrderItems(order, dto.cartItems());

        orderRepository.save(order);

        //notificationService.sendTelegram();

        return orderMapper.toDto(order);
    }

    private void enrichOrderWithMeta(Order order, Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
            UUID userId = UUID.fromString(authentication.getName());
            User user = userService.getActiveUserEntityByPublicId(userId);
            order.setUser(user);
        }

        Long seqNumber = orderRepository.getNextOrderNumber();

        order.setOrderNumber(String.valueOf(seqNumber));
    }

    private void processOrderItems(Order order, List<CartItemDTO> cartItems) {

        List<OrderItem> items = cartItems.stream()
                .map(dto -> createAndValidateOrderItem(dto, order))
                .toList();

        order.setItems(new ArrayList<>(items));

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getFinalPricePerUnit()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
    }

    private OrderItem createAndValidateOrderItem(CartItemDTO itemDto, Order order) {
        Product product = productService.findById(itemDto.productId());

        if (product.getAvailability() != ProductAvailability.IN_STOCK) {
            throw new OutOfStockException("Product " + product.getName() + " is not available");
        }

        return orderMapper.toOrderItem(itemDto, product, order);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(UUID publicId, OrderUpdateDTO dto) {
        Order order = getOrderEntityByPublicId(publicId);

        validateUserAccess(order, SecurityContextHolder.getContext().getAuthentication());

        if (order.getOrderStatus() != OrderStatus.NEW &&
                order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot update order details at this stage");
        }

        order.setEmail(dto.email());
        order.setPhoneNumber(dto.phoneNumber());
        order.setFirstName(dto.firstName());
        order.setSecondName(dto.secondName());
        order.setAddress(orderMapper.toAddress(dto.addressDTO()));

        orderRepository.save(order);

        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID publicId) {
        Order order = getOrderEntityByPublicId(publicId);

        validateUserAccess(order, SecurityContextHolder.getContext().getAuthentication());

        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DONE) {
            throw new IllegalStateException("Cannot cancel order that is already shipped");
        }

        order.setOrderStatus(OrderStatus.CANCELED);

//        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
//             paymentService.initiateRefund(order);
//            order.setPaymentStatus(PaymentStatus.REFUNDED);
//        }

        orderRepository.save(order);
    }

    private void validateUserAccess(Order order, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_" + RoleType.ADMIN.name())
                                || a.getAuthority().equals(RoleType.ADMIN.name()))
        ) return;


        UUID currentUserId = UUID.fromString(authentication.getName());

        if (order.getUser() == null || !order.getUser().getPublicId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied: You are not the owner of this order");
        }
    }


    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByPublicId(UUID publicId) {
        return orderMapper.toDto(getOrderEntityByPublicId(publicId));
    }

    private Order getOrderEntityByPublicId(UUID publicId) {
        return orderRepository.findOrderByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByUserId(UUID userId, int page, int size) {
        return orderRepository.findAllByUser_PublicId(userId, PageRequest.of(page, size, Sort.by("createAt"))).map(orderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(int page, int size) {
        return orderRepository.findAll(PageRequest.of(page, size, Sort.by("createAt"))).map(orderMapper::toDto);
    }
}
