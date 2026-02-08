package ua.moki.modules.orders.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.orders.dtos.*;
import ua.moki.modules.orders.services.OrderService;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.enums.PaymentStatus;
import ua.moki.modules.orders.utils.enums.PaymentType;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.OutOfStockException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private AccessTokenJwsStringDeserializer accessTokenDeserializer;

    @BeforeEach
    void setUp() throws ServletException, IOException {

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST /orders creates an order and returns 201 Created")
    void createOrder_shouldReturnCreated() throws Exception {

        OrderRequestDTO requestDTO = createSampleRequestDTO();
        OrderResponseDTO responseDTO = createSampleResponseDTO(UUID.randomUUID(), "ORD-123");

        when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/orders")
                        .with(user("user").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123"));
    }

    @Test
    @DisplayName("POST /orders creates an order and returns 201 Created for an anonymous user")
    void createOrder_shouldReturnCreated_withAnonymUser() throws Exception {

        OrderRequestDTO requestDTO = createSampleRequestDTO();
        OrderResponseDTO responseDTO = createSampleResponseDTO(UUID.randomUUID(), "ORD-123");

        when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123"));
    }

    @Test
    @DisplayName("POST /orders return 400 BadRequest when product is out of stock")
    void createOrder_shouldReturnBadRequest_whenThrowOutOfStockException() throws Exception {

        OrderRequestDTO requestDTO = createSampleRequestDTO();

        when(orderService.createOrder(any(OrderRequestDTO.class)))
                .thenThrow(new OutOfStockException("Product is out of stock"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /orders/{id} updates an order and returns 200 OK")
    @WithMockUser // Імітуємо автентифікованого користувача
    void updateOrder_shouldReturnOk() throws Exception {

        UUID orderId = UUID.randomUUID();
        OrderUpdateDTO updateDTO = createSampleUpdateDTO();
        OrderResponseDTO responseDTO = createSampleResponseDTO(orderId, "ORD-123");

        when(orderService.updateOrder(any(UUID.class), any(OrderUpdateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(patch("/orders/{id}", orderId)
                        .with(user("user").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.email").value(responseDTO.email()));
    }

    @Test
    @DisplayName("PATCH /orders/{id} returns 401 Unauthorized for anonymous user")
    void updateOrder_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderUpdateDTO updateDTO = createSampleUpdateDTO();

        mockMvc.perform(patch("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /orders/{id} returns 400 BadRequest when validation fails")
    void updateOrder_shouldReturnBadRequest_whenInvalidData() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderUpdateDTO invalidDTO = new OrderUpdateDTO(
                "invalid-email",
                "not-a-phone",
                "",
                "Surname",
                DeliveryType.NOVA_POSHTA,
                PaymentType.CARD,
                OrderStatus.NEW,
                PaymentStatus.SUCCESS,
                new AddressDTO("Kyiv", "Kyivskyi", "1", "Street", "1", "1")
        );

        mockMvc.perform(patch("/orders/{id}", orderId)
                        .with(user("user").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /orders/{id} returns 404 Not Found when order does not exist")
    void updateOrder_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderUpdateDTO updateDTO = createSampleUpdateDTO();

        when(orderService.updateOrder(any(UUID.class), any(OrderUpdateDTO.class)))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(patch("/orders/{id}", orderId)
                        .with(user("user").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /orders/cancel/{id} cancels an order and returns 204 No Content")
    void cancelOrder_shouldReturnNoContent() throws Exception {

        UUID orderId = UUID.randomUUID();

        doNothing().when(orderService).cancelOrder(orderId);

        mockMvc.perform(delete("/orders/cancel/{id}", orderId)
                        .with(user("user").roles("CUSTOMER")))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).cancelOrder(orderId);
    }

    @Test
    @DisplayName("DELETE /orders/cancel/{id} returns 401 Unauthorized for anonymous user")
    void cancelOrder_shouldReturnUnauthorized_whenAnonymous() throws Exception {

        UUID orderId = UUID.randomUUID();

        mockMvc.perform(delete("/orders/cancel/{id}", orderId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("DELETE /orders/cancel/{id} returns 404 Not Found when order does not exist")
    void cancelOrder_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {

        UUID orderId = UUID.randomUUID();

        doThrow(new EntityNotFoundException("Order not found"))
                .when(orderService).cancelOrder(orderId);

        mockMvc.perform(delete("/orders/cancel/{id}", orderId)
                .with(user("user").roles("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /orders/{id} returns order and 200 OK for authenticated user")
    void getOrderById_shouldReturnOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        OrderResponseDTO responseDTO = createSampleResponseDTO(orderId, "ORD-555");

        when(orderService.getOrderByPublicId(orderId)).thenReturn(responseDTO);

        mockMvc.perform(get("/orders/{id}", orderId)
                        .with(user("user").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-555"))
                .andExpect(jsonPath("$.email").value(responseDTO.email()));

        verify(orderService, times(1)).getOrderByPublicId(orderId);
    }

    @Test
    @DisplayName("GET /orders/{id} returns 401 Unauthorized for anonymous user")
    void getOrderById_shouldReturnUnauthorized_whenAnonymous() throws Exception {

        UUID orderId = UUID.randomUUID();

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/{id} returns 404 Not Found when order does not exist")
    void getOrderById_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {

        UUID orderId = UUID.randomUUID();

        when(orderService.getOrderByPublicId(orderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/orders/{id}", orderId)
                        .with(user("user").roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrderByPublicId(orderId);
    }

    @Test
    @DisplayName("GET /orders/user returns paged orders for the authenticated user")
    void getOrdersByUser_shouldReturnPagedOrders() throws Exception {

        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;
        OrderResponseDTO orderResponse = createSampleResponseDTO(UUID.randomUUID(), "ORD-100");

        Page<OrderResponseDTO> orderPage = new PageImpl<>(List.of(orderResponse));

        when(orderService.getOrdersByUserId(eq(userId), eq(page), eq(size)))
                .thenReturn(orderPage);

        mockMvc.perform(get("/orders/user")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-100"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(orderService).getOrdersByUserId(userId, page, size);
    }

    @Test
    @DisplayName("GET /orders/user returns 401 Unauthorized for anonymous user")
    void getOrdersByUser_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(get("/orders/user")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/user returns 400 BadRequest when parameters are invalid")
    void getOrdersByUser_shouldReturnBadRequest_whenInvalidParams() throws Exception {
        mockMvc.perform(get("/orders/user")
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders returns all orders for ADMIN role")
    void getAllOrders_shouldReturnPagedOrders_whenAdmin() throws Exception {
        // Given
        int page = 0;
        int size = 10;
        OrderResponseDTO orderResponse = createSampleResponseDTO(UUID.randomUUID(), "ORD-ADMIN");
        Page<OrderResponseDTO> orderPage = new PageImpl<>(List.of(orderResponse));

        when(orderService.getAllOrders(page, size)).thenReturn(orderPage);

        mockMvc.perform(get("/orders")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-ADMIN"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(orderService).getAllOrders(page, size);
    }

    @Test
    @DisplayName("GET /orders returns all orders for MANAGER role")
    void getAllOrders_shouldReturnPagedOrders_whenManager() throws Exception {
        // Given
        int page = 0;
        int size = 5;
        Page<OrderResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        when(orderService.getAllOrders(page, size)).thenReturn(emptyPage);

        mockMvc.perform(get("/orders")
                        .with(user("manager").roles("MANAGER"))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /orders returns 403 Forbidden for CUSTOMER role")
    void getAllOrders_shouldReturnForbidden_whenCustomer() throws Exception {
        mockMvc.perform(get("/orders")
                        .with(user("user").roles("CUSTOMER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders returns 401 Unauthorized for anonymous user")
    void getAllOrders_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        // When & Then
        mockMvc.perform(get("/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /orders returns 400 BadRequest when validation fails")
    void getAllOrders_shouldReturnBadRequest_whenInvalidParams() throws Exception {

        mockMvc.perform(get("/orders")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    private OrderRequestDTO createSampleRequestDTO() {
        CartItemDTO item = new CartItemDTO(1L, 2);
        AddressDTO address = new AddressDTO("Kyiv", "Kyivskyi", "1", "Main st", "1","1");

        return new OrderRequestDTO(
                "order@GMAIL.com",
                "+380000000000",
                "Name",
                "Surname",
                DeliveryType.NOVA_POSHTA,
                PaymentType.CARD,
                List.of(item),
                address
        );
    }

    private OrderUpdateDTO createSampleUpdateDTO() {
        AddressDTO address = new AddressDTO("Kyiv", "Kyivskyi", "1", "Main st", "1", "1");
        return new OrderUpdateDTO(
                "updated@test.com",
                "+380991112233",
                "Newname",
                "Newsurname",
                DeliveryType.NOVA_POSHTA,
                PaymentType.CARD,
                OrderStatus.NEW,
                PaymentStatus.SUCCESS,
                address
        );
    }

    private OrderResponseDTO createSampleResponseDTO(UUID id, String number) {
        return new OrderResponseDTO(
                id,
                number,
                OffsetDateTime.now(),
                OrderStatus.NEW,
                PaymentType.CARD,
                PaymentStatus.SUCCESS,
                DeliveryType.NOVA_POSHTA,
                new AddressDTO("City", "Region", "1", "Street", "1", "1"),
                "email@test.com",
                "+380991112233",
                "First",
                "Last",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(90.00),
                Collections.emptyList()
        );
    }
}
