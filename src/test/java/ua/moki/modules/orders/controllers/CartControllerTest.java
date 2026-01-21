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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("POST /carts/add should return 200 OK and cart data for authenticated user")
    void addToCart_shouldReturnOk() throws Exception {

        UUID userId = UUID.randomUUID();
        Long productId = 1L;
        int quantity = 2;
        CartResponseDTO responseDTO = new CartResponseDTO(UUID.randomUUID(), BigDecimal.ONE, Collections.emptyList());

        when(cartService.addToCart(eq(userId), eq(productId), eq(quantity))).thenReturn(responseDTO);

        mockMvc.perform(post("/carts/add")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("productId", productId.toString())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").exists());

        verify(cartService).addToCart(userId, productId, quantity);
    }

    @Test
    @DisplayName("POST /carts/add should return 400 BadRequest when quantity < 1")
    void addToCart_shouldReturnBadRequest_whenInvalidQuantity() throws Exception {
        mockMvc.perform(post("/carts/add")
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .param("productId", "1")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/add should return 401 Unauthorized for anonymous user")
    void addToCart_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/carts/add")
                        .param("productId", "1")
                        .param("quantity", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /carts/clear should return 204 No Content for authenticated user")
    void clearCart_shouldReturnNoContent() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/carts/clear")
                        .with(user(userId.toString()).roles("CUSTOMER")))
                .andExpect(status().isNoContent());

        verify(cartService, times(1)).clearCart(userId);
    }

    @Test
    @DisplayName("DELETE /carts/clear should return 401 Unauthorized for anonymous user")
    void clearCart_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/carts/clear"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }
}
