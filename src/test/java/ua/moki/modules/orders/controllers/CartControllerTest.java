package ua.moki.modules.orders.controllers;

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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.orders.services.CartService;
import ua.moki.modules.orders.services.OrderService;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

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
    @DisplayName("POST /cart/items should return 200 OK and cart data for authenticated user")
    void addToCart_shouldReturnOk() throws Exception {

        UUID userId = UUID.randomUUID();
        Long productId = 1L;
        int quantity = 2;
        CartResponseDTO responseDTO = new CartResponseDTO(UUID.randomUUID(), BigDecimal.ONE, Collections.emptyList());

        when(cartService.addToCart(eq(userId), eq(productId), eq(quantity), isNull())).thenReturn(responseDTO);

        mockMvc.perform(post("/cart/items")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("productId", productId.toString())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists());

        verify(cartService).addToCart(userId, productId, quantity, null);
    }

    @Test
    @DisplayName("POST /cart/items should return 400 BadRequest when quantity < 1")
    void addToCart_shouldReturnBadRequest_whenInvalidQuantity() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .param("productId", "1")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items should return 401 Unauthorized for anonymous user")
    void addToCart_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("productId", "1")
                        .param("quantity", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /cart/items/{productId} should update item quantity and return 200 OK")
    void updateQuantity_shouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        Long productId = 5L;
        int quantity = 3;
        CartResponseDTO responseDTO = new CartResponseDTO(UUID.randomUUID(), BigDecimal.TEN, Collections.emptyList());

        when(cartService.updateItemQuantity(userId, productId, quantity)).thenReturn(responseDTO);

        mockMvc.perform(put("/cart/items/{productId}", productId)
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists());

        verify(cartService).updateItemQuantity(userId, productId, quantity);
    }

    @Test
    @DisplayName("DELETE /cart/items/{productId} should remove item from cart and return 200 OK")
    void deleteItemFromCart_shouldReturnOk() throws Exception {

        UUID userId = UUID.randomUUID();
        Long productId = 10L;
        CartResponseDTO responseDTO = new CartResponseDTO(UUID.randomUUID(), BigDecimal.ZERO, Collections.emptyList());

        when(cartService.deleteItemFromCart(userId, productId)).thenReturn(responseDTO);

        mockMvc.perform(delete("/cart/items/{productId}", productId)
                        .with(user(userId.toString()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists());

        verify(cartService, times(1)).deleteItemFromCart(userId, productId);
    }

    @Test
    @DisplayName("DELETE /cart/items/{productId} should return 401 Unauthorized for anonymous user")
    void deleteItemFromCart_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/cart/items/{productId}", 10L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET /cart should return current user's cart")
    void getCart_shouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        CartResponseDTO responseDTO = new CartResponseDTO(UUID.randomUUID(), BigDecimal.ZERO, Collections.emptyList());

        when(cartService.getCart(userId)).thenReturn(responseDTO);

        mockMvc.perform(get("/cart")
                        .with(user(userId.toString()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists());

        verify(cartService).getCart(userId);
    }
}
