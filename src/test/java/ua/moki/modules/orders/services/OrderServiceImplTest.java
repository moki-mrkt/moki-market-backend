package ua.moki.modules.orders.services;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.orders.domains.Order;
import ua.moki.modules.orders.dtos.*;
import ua.moki.modules.orders.repositories.OrderRepository;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.enums.PaymentStatus;
import ua.moki.modules.orders.utils.enums.PaymentType;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.OutOfStockException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createOrder successfully creates an order for an anonymous user")
    void createOrder_shouldCreateOrder_whenUserIsAnonymous() {

        Product product = createAndSaveProduct("Phone", BigDecimal.valueOf(1000), ProductAvailability.IN_STOCK);

        CartItemDTO itemDTO = new CartItemDTO(product.getId(), 2);
        OrderRequestDTO requestDTO = createOrderRequest(List.of(itemDTO));

        OrderResponseDTO response = orderService.createOrder(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isNotNull();
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(2000));

        Order savedOrder = orderRepository.findOrderByPublicId(response.id()).orElseThrow();
        assertThat(savedOrder.getUser()).isNull(); // Анонімне замовлення
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getItems().getFirst().getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("createOrder successfully creates an order and binds the authorized user")
    void createOrder_shouldLinkUser_whenUserIsAuthenticated() {

        User user = createAndSaveUser("Auth", RoleType.CUSTOMER, "auth.order@test.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getPublicId().toString(), null, List.of())
        );

        Product product = createAndSaveProduct("Laptop", BigDecimal.valueOf(5000), ProductAvailability.IN_STOCK);
        OrderRequestDTO requestDTO = createOrderRequest(List.of(new CartItemDTO(product.getId(), 1)));

        OrderResponseDTO response = orderService.createOrder(requestDTO);

        Order savedOrder = orderRepository.findOrderByPublicId(response.id()).orElseThrow();
        assertThat(savedOrder.getUser()).isNotNull();
        assertThat(savedOrder.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedOrder.getOrderNumber()).isNotNull();
    }

    @Test
    @DisplayName("createOrder throws OutOfStockException if the product is out of stock")
    void createOrder_shouldThrowException_whenProductIsOutOfStock() {

        Product product = createAndSaveProduct("SoldOutItem", BigDecimal.valueOf(50), ProductAvailability.OUT_OF_STOCK);

        OrderRequestDTO requestDTO = createOrderRequest(List.of(new CartItemDTO(product.getId(), 1)));

        assertThatThrownBy(() -> orderService.createOrder(requestDTO))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("is not available");
    }

    @Test
    @DisplayName("createOrder throws EntityNotFoundException if product ID is invalid")
    void createOrder_shouldThrowException_whenProductNotFound() {

        Long nonExistentId = 99999L;
        OrderRequestDTO requestDTO = createOrderRequest(List.of(new CartItemDTO(nonExistentId, 1)));

        assertThatThrownBy(() -> orderService.createOrder(requestDTO))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("createOrder correctly calculates the total for multiple products")
    void createOrder_shouldCalculateTotalAmountCorrectly() {

        Product p1 = createAndSaveProduct("Item 1", BigDecimal.valueOf(100), ProductAvailability.IN_STOCK);
        Product p2 = createAndSaveProduct("Item 2", BigDecimal.valueOf(50), ProductAvailability.IN_STOCK);

        List<CartItemDTO> items = List.of(
                new CartItemDTO(p1.getId(), 2),
                new CartItemDTO(p2.getId(), 3)
        );

        OrderRequestDTO requestDTO = createOrderRequest(items);

        OrderResponseDTO response = orderService.createOrder(requestDTO);

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(350));
    }

    @Test
    @DisplayName("updateOrder successfully updates order details when status is NEW and user is logged in")
    void updateOrder_shouldUpdateDetails_whenOrderIsNewAndUserIsOwner() {


        User user = createAndSaveUser("Owner", RoleType.CUSTOMER, "owner@test.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getPublicId().toString(), null, List.of())
        );

        Order order = createAndSaveOrder(OrderStatus.NEW, user, "ORD-1");

        orderRepository.save(order);

        AddressDTO newAddress = new AddressDTO("City", "region", "1", "street", "1", "1");
        OrderUpdateDTO updateDTO = new OrderUpdateDTO(
                "new.email@test.com", "+380992222222", "NewName", "NewSurname", DeliveryType.NOVA_POSHTA, PaymentType.CARD,
                OrderStatus.NEW,PaymentStatus.PENDING, newAddress
        );

        OrderResponseDTO result = orderService.updateOrder(order.getPublicId(), updateDTO);

        assertThat(result.email()).isEqualTo("new.email@test.com");
        assertThat(result.firstName()).isEqualTo("NewName");

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getEmail()).isEqualTo("new.email@test.com");
        assertThat(updatedOrder.getPhoneNumber()).isEqualTo("+380992222222");
        assertThat(updatedOrder.getFirstName()).isEqualTo("NewName");
        assertThat(updatedOrder.getSecondName()).isEqualTo("NewSurname");

        assertThat(updatedOrder.getAddress().getCity()).isEqualTo("City");
    }

    @Test
    @DisplayName("updateOrder throws an AccessDeniedException (or similar) when a user tries to update someone else's order")
    void updateOrder_shouldThrowException_whenUserIsNotOwner() {

        User owner = createAndSaveUser("Owner", RoleType.CUSTOMER, "owner@test.com");

        Order order = createAndSaveOrder(OrderStatus.SHIPPED, owner, "ORD-1");

        User hacker = createAndSaveUser("Hacker", RoleType.CUSTOMER, "hacker@test.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(hacker.getPublicId().toString(), null, List.of())
        );

        OrderUpdateDTO updateDTO = new OrderUpdateDTO(
                "hacked@test.com", "000", "Hacked", "Name", DeliveryType.NOVA_POSHTA, PaymentType.CARD,
        OrderStatus.NEW,PaymentStatus.PENDING, null
        );

        assertThatThrownBy(() -> orderService.updateOrder(order.getPublicId(), updateDTO))
                .isInstanceOfAny(
                        AccessDeniedException.class,
                        EntityNotFoundException.class,
                        SecurityException.class
                );
    }

    @Test
    @DisplayName("cancelOrder successfully cancels an order when the owner makes a request")
    void cancelOrder_shouldCancelOrder_whenUserIsOwner() {

        User owner = createAndSaveUser("Owner", RoleType.CUSTOMER, "owner@test.com");

        Order order = createAndSaveOrder(OrderStatus.NEW, owner, "ORD-1");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getPublicId().toString(), null, List.of())
        );

        orderService.cancelOrder(order.getPublicId());

        Order canceledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(canceledOrder.getOrderStatus()).isEqualTo(ua.moki.modules.orders.utils.enums.OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("cancelOrder successfully cancels someone else's order when requested by ADMIN")
    void cancelOrder_shouldCancelOrder_whenUserIsAdmin() {


        User owner = createAndSaveUser("Owner", RoleType.CUSTOMER, "owner@test.com");

        Order order = createAndSaveOrder(OrderStatus.CONFIRMED, owner, "ORD-1");

        User admin = createAndSaveUser("Admin", RoleType.ADMIN, "admin.cancel@test.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getPublicId().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        orderService.cancelOrder(order.getPublicId());

        Order canceledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(canceledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("cancelOrder throws IllegalStateException if the order has already been shipped (SHIPPED)")
    void cancelOrder_shouldThrowException_whenOrderIsShipped() {

        User owner = createAndSaveUser("Owner", RoleType.CUSTOMER, "user.target@test.com");

        Order order = createAndSaveOrder(OrderStatus.SHIPPED, owner, "ORD-1");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getPublicId().toString(), null, List.of())
        );

        assertThatThrownBy(() -> orderService.cancelOrder(order.getPublicId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order that is already shipped");
    }

    @Test
    @DisplayName("cancelOrder throws AccessDeniedException when a user tries to cancel someone else's order")
    void cancelOrder_shouldThrowException_whenUserIsNotOwner() {

        User owner = createAndSaveUser("Owner", RoleType.CUSTOMER, "user.target@test.com");

        Order order = createAndSaveOrder(OrderStatus.CONFIRMED, owner, "ORD-1");

        User hacker = createAndSaveUser("Hacker", RoleType.CUSTOMER, "hacker.cancel@test.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(hacker.getPublicId().toString(), null, List.of())
        );

        assertThatThrownBy(() -> orderService.cancelOrder(order.getPublicId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getOrderByPublicId повертає DTO замовлення, якщо воно існує")
    void getOrderByPublicId_shouldReturnDto_whenOrderExists() {

        User user = createAndSaveUser("TestReceiver", RoleType.CUSTOMER, "get.order@test.com");

        Order order = createAndSaveOrder(OrderStatus.NEW, user, "ORD-1");

        OrderResponseDTO result = orderService.getOrderByPublicId(order.getPublicId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(order.getPublicId());
        assertThat(result.orderNumber()).isEqualTo("ORD-1");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(result.firstName()).isEqualTo("OldName");
    }

    @Test
    @DisplayName("getOrderByPublicId кидає EntityNotFoundException, якщо замовлення не знайдено")
    void getOrderByPublicId_shouldThrowException_whenOrderDoesNotExist() {

        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.getOrderByPublicId(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getOrdersByUserId returns orders with pagination and sorting")
    void getOrdersByUserId_shouldReturnPagedAndSortedOrders() throws InterruptedException {

        User user = createAndSaveUser("PagedUser", RoleType.CUSTOMER, "paged.user@test.com");


        createAndSaveOrder(OrderStatus.NEW, user, "ORD-1");
        Thread.sleep(50);
        createAndSaveOrder(OrderStatus.NEW, user, "ORD-2");
        Thread.sleep(50);
        createAndSaveOrder(OrderStatus.NEW, user, "ORD-3");

        Page<OrderResponseDTO> pageResult = orderService.getOrdersByUserId(user.getPublicId(), 0, 2);

        assertThat(pageResult).isNotNull();
        assertThat(pageResult.getTotalElements()).isEqualTo(3);
        assertThat(pageResult.getContent()).hasSize(2);

        assertThat(pageResult.getContent().get(0).orderNumber()).isEqualTo("ORD-1");
        assertThat(pageResult.getContent().get(1).orderNumber()).isEqualTo("ORD-2");

        Page<OrderResponseDTO> secondPage = orderService.getOrdersByUserId(user.getPublicId(), 1, 2);

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().orderNumber()).isEqualTo("ORD-3");
    }

    @Test
    @DisplayName("getOrdersByUserId returns only orders from the specified user (data isolation)")
    void getOrdersByUserId_shouldReturnOrdersOnlyForSpecificUser() {

        User targetUser = createAndSaveUser("TargetUser", RoleType.CUSTOMER, "target@test.com");

        User otherUser = createAndSaveUser("OtherUser", RoleType.CUSTOMER, "other@test.com");

        createAndSaveOrder(OrderStatus.NEW, targetUser, "TARGET-1");
        createAndSaveOrder(OrderStatus.NEW, targetUser, "TARGET-2");

        createAndSaveOrder(OrderStatus.NEW, otherUser, "OTHER-1");

        Page<OrderResponseDTO> result = orderService.getOrdersByUserId(targetUser.getPublicId(), 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(OrderResponseDTO::orderNumber)
                .containsExactlyInAnyOrder("TARGET-1", "TARGET-2")
                .doesNotContain("OTHER-1");
    }

    @Test
    @DisplayName("getOrdersByUserId returns an empty page if the user has no orders")
    void getOrdersByUserId_shouldReturnEmptyPage_whenNoOrdersExist() {

        User user = createAndSaveUser("User", RoleType.CUSTOMER, "empty@test.com");

        Page<OrderResponseDTO> result = orderService.getOrdersByUserId(user.getPublicId(), 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getAllOrders returns all orders with pagination and sorting")
    void getAllOrders_shouldReturnPagedAndSortedOrders() {

        User user1 = createAndSaveUser("User1", RoleType.CUSTOMER, "user1.all@test.com");
        User user2 = createAndSaveUser("User2", RoleType.CUSTOMER, "user2.all@test.com");

        createAndSaveOrder(OrderStatus.NEW, user1, "ORD-ALL-1");
        createAndSaveOrder(OrderStatus.NEW, user2, "ORD-ALL-2");
        createAndSaveOrder(OrderStatus.NEW, user1, "ORD-ALL-3");

        Page<OrderResponseDTO> resultPage = orderService.getAllOrders(0, 2);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(3);
        assertThat(resultPage.getContent()).hasSize(2);

        assertThat(resultPage.getContent().get(0).orderNumber()).isEqualTo("ORD-ALL-1");
        assertThat(resultPage.getContent().get(1).orderNumber()).isEqualTo("ORD-ALL-2");
    }

    @Test
    @DisplayName("getAllOrders returns an empty page if there are no orders in the database")
    void getAllOrders_shouldReturnEmptyPage_whenDbIsEmpty() {

        Page<OrderResponseDTO> result = orderService.getAllOrders(0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private Product createAndSaveProduct(String name, BigDecimal price, ProductAvailability availability) {
        Product product = new Product();
        product.setName(name);
        product.setProductCategory(ProductCategory.DRIED_FRUITS);
        product.setDescription("Test product");
        product.setPurchasePrice(BigDecimal.valueOf(100));
        product.setDiscount(0);
        product.setPrice(price);
        product.setAvailability(availability);
        product.setInitOfMeasure("шт");
        product.setValueOfInitOfMeasure(1);
        product.setManufacturerOfTheProduct("Test Factory");
        product.setSubcategory("General");
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());

        return productRepository.save(product);
    }

    private User createAndSaveUser(String name, RoleType roleType, String email) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName(name);
        user.setSecondName("User");
        user.setEmail(email);
        user.setPhoneNumber("+380991111111");
        user.setPassword("pass");
        user.setRoleType(roleType);
        user.setDeleted(false);
        return userRepository.save(user);
    }

    private Order createAndSaveOrder(OrderStatus orderStatus, User user, String orderNumber) {
        Order order = new Order();
        order.setPublicId(UUID.randomUUID());
        order.setUser(user);
        order.setOrderStatus(orderStatus);
        order.setTotalAmount(BigDecimal.TEN);
        order.setOrderNumber(orderNumber);

        order.setFirstName("OldName");
        order.setEmail("old@test.com");
        order.setPhoneNumber("+380991111111");
        order.setSecondName("OldSurname");
        order.setDeliveryType(DeliveryType.NOVA_POSHTA);
        order.setPaymentType(PaymentType.CARD);
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        return orderRepository.save(order);
    }

    private OrderRequestDTO createOrderRequest(List<CartItemDTO> items) {
        AddressDTO address = new AddressDTO("Kyiv", "Kyivskyi", "1", "Main st", "1","1");

        return new OrderRequestDTO(
                "order@GMAIL.com",
                "+380000000000",
                "Name",
                "Surname",
                DeliveryType.NOVA_POSHTA,
                PaymentType.CARD,
                items,
                address
        );
    }
}
