package ua.moki.modules.products.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.products.dtos.*;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST /products - 201 Created for ADMIN")
    void createProduct_shouldReturnCreated_whenAdminAndValidRequest() throws Exception {
        ProductRequestDTO requestDTO = createValidProductRequestDTO();
        ProductResponseDTO responseDTO = createSampleProductResponseDTO(100L);

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/products/100"))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is(requestDTO.name())));

        verify(productService).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("POST /products - 403 Forbidden for MANAGER (only ADMIN)")
    void createProduct_shouldReturnForbidden_whenManager() throws Exception {
        ProductRequestDTO requestDTO = createValidProductRequestDTO();

        mockMvc.perform(post("/products")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("POST /products - 400 Bad Request on validation failure (e.g. negative price)")
    void createProduct_shouldReturnBadRequest_whenDataInvalid() throws Exception {
        ProductRequestDTO invalidDTO = new ProductRequestDTO(
                "A",
                "А",
                ProductCategory.NUTS,
                "Desc",
                "Опис",
                new BigDecimal("-10"),
                ProductAvailability.IN_STOCK,
                105,
                new BigDecimal("100"),
                "Maker",
                "Sub",
                "Підкат",
                "WRONG",
                1000,
                null,
                null,
                null,
                ProductType.SIMPLE,
                null,
                false,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("PUT /products/{id} - 200 OK for MANAGER")
    void updateProduct_shouldReturnOk_whenManager() throws Exception {
        Long productId = 1L;
        ProductRequestDTO requestDTO = createValidProductRequestDTO();
        ProductResponseDTO responseDTO = createSampleProductResponseDTO(productId);

        when(productService.updateProduct(eq(productId), any(ProductRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/products/{id}", productId)
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(productService).updateProduct(eq(productId), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /products/{id} - 403 Forbidden for CUSTOMER")
    void updateProduct_shouldReturnForbidden_whenCustomer() throws Exception {
        ProductRequestDTO requestDTO = createValidProductRequestDTO();

        mockMvc.perform(put("/products/{id}", 1L)
                        .with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @DisplayName("DELETE /products/{id} - 204 No Content for ADMIN")
    void deleteProduct_shouldReturnNoContent_whenAdmin() throws Exception {
        Long productId = 5L;
        doNothing().when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/products/{id}", productId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(productId);
    }

    @Test
    @DisplayName("DELETE /products/{id} - 404 Not Found if product does not exist")
    void deleteProduct_shouldReturnNotFound_whenIdDoesNotExist() throws Exception {
        Long productId = 999L;
        doThrow(new EntityNotFoundException("Product not found")).when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/products/{id}", productId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /products/{id} - 200 OK for anonymous user")
    void getProductById_shouldReturnProduct_whenPublic() throws Exception {
        Long productId = 10L;
        ProductResponseDTO responseDTO = createSampleProductResponseDTO(productId);

        when(productService.getProductById(productId)).thenReturn(responseDTO);

        mockMvc.perform(get("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("Valid Product Name")));

        verify(productService).getProductById(productId);
    }

    @Test
    @DisplayName("GET /products/{id}/admin - 200 OK for ADMIN")
    void getProductByIdForAdmin_shouldReturnAdminDto_whenAdmin() throws Exception {
        Long productId = 10L;
        ProductAdminResponseDTO adminDTO = createSampleProductAdminResponseDTO(productId);

        when(productService.getProductByIdForAdmin(productId)).thenReturn(adminDTO);

        mockMvc.perform(get("/products/{id}/admin", productId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("Valid Product Name")))
                .andExpect(jsonPath("$.nameRu", is("Валидное имя товара")));

        verify(productService).getProductByIdForAdmin(productId);
    }

    @Test
    @DisplayName("GET /products/{id}/admin - 403 Forbidden for CUSTOMER")
    void getProductByIdForAdmin_shouldReturnForbidden_whenCustomer() throws Exception {
        mockMvc.perform(get("/products/{id}/admin", 10L)
                        .with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(productService, never()).getProductByIdForAdmin(any());
    }

    @Test
    @DisplayName("GET /products/slug/{slug} - 200 OK for anonymous user")
    void getProductBySlug_shouldReturnProduct_whenPublic() throws Exception {
        String slug = "valid-product-name";
        ProductResponseDTO responseDTO = createSampleProductResponseDTO(10L);

        when(productService.getProductBySlug(slug)).thenReturn(responseDTO);

        mockMvc.perform(get("/products/slug/{slug}", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.slug", is(slug)));

        verify(productService).getProductBySlug(slug);
    }

    @Test
    @DisplayName("GET /products - returns Page<ProductAdminResponseDTO> for ADMIN")
    void getAllProducts_shouldReturnPageOfProducts() throws Exception {
        ProductAdminResponseDTO dto = createSampleProductAdminResponseDTO(1L);
        Page<ProductAdminResponseDTO> page = new PageImpl<>(List.of(dto));

        when(productService.getAllProducts(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/products")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)));

        verify(productService).getAllProducts(eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /products/public/sitemap - returns Page<ProductPublicDTO> for public")
    void getAllProductsForPublic_shouldReturnPublicSitemapPage() throws Exception {
        ProductPublicDTO publicDTO = new ProductPublicDTO(1L, "valid-product-name");
        Page<ProductPublicDTO> page = new PageImpl<>(List.of(publicDTO));

        when(productService.getAllProductsForPublic(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/products/public/sitemap")
                        .param("page", "0")
                        .param("size", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug", is("valid-product-name")));

        verify(productService).getAllProductsForPublic(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /products/category/{category} - filters by category")
    void getAllProductsByCategory_shouldReturnFilteredPage() throws Exception {
        ProductCategory category = ProductCategory.SWEETS;
        int pageNum = 0;
        int size = 5;

        ProductResponseDTO dto = createSampleProductResponseDTO(1L);
        Page<ProductResponseDTO> page = new PageImpl<>(List.of(dto));

        when(productService.getAllProductByCategory(category, pageNum, size)).thenReturn(page);

        mockMvc.perform(get("/products/category/{category}", category)
                        .param("page", String.valueOf(pageNum))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(productService).getAllProductByCategory(category, pageNum, size);
    }

    @Test
    @DisplayName("GET /products/new - 400 Bad Request when page < 0")
    void getNewProducts_shouldReturnBadRequest_whenPageIsNegative() throws Exception {
        mockMvc.perform(get("/products/new")
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getNewProducts(anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /products/discount - returns discounted products")
    void getProductsWithDiscount_shouldReturnPage() throws Exception {
        int pageNum = 0;
        int size = 10;
        Page<ProductResponseDTO> page = new PageImpl<>(Collections.emptyList());

        when(productService.getProductsWithDiscount(pageNum, size)).thenReturn(page);

        mockMvc.perform(get("/products/discount")
                        .param("page", String.valueOf(pageNum))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(productService).getProductsWithDiscount(pageNum, size);
    }

    @Test
    @DisplayName("GET /products/bestsellers - returns bestseller products")
    void getBestsellers_shouldReturnPage() throws Exception {
        int pageNum = 0;
        int size = 10;
        Page<ProductResponseDTO> page = new PageImpl<>(Collections.emptyList());

        when(productService.getBestsellers(pageNum, size)).thenReturn(page);

        mockMvc.perform(get("/products/bestsellers")
                        .param("page", String.valueOf(pageNum))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk());

        verify(productService).getBestsellers(pageNum, size);
    }

    @Test
    @DisplayName("GET /products/search - returns SearchResponse for query and filters")
    void search_shouldReturnSearchResponse() throws Exception {
        ProductResponseDTO dto = createSampleProductResponseDTO(1L);
        SearchResponse searchResponse = new SearchResponse(
                new PageImpl<>(List.of(dto)),
                List.of("Subcategory"),
                50.0,
                200.0
        );

        when(productService.searchProducts(any(ProductSearchRequestDTO.class), any(Pageable.class)))
                .thenReturn(searchResponse);

        mockMvc.perform(get("/products/search")
                        .param("query", "матча")
                        .param("category", "TEA")
                        .param("minPrice", "50")
                        .param("maxPrice", "200")
                        .param("page", "0")
                        .param("size", "12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.content", hasSize(1)))
                .andExpect(jsonPath("$.subcategories", hasSize(1)))
                .andExpect(jsonPath("$.minPrice", is(50.0)))
                .andExpect(jsonPath("$.maxPrice", is(200.0)));

        verify(productService).searchProducts(any(ProductSearchRequestDTO.class), any(Pageable.class));
    }

    private ProductRequestDTO createValidProductRequestDTO() {
        return new ProductRequestDTO(
                "Valid Product Name",
                "Валидное имя товара",
                ProductCategory.NUTS,
                "Valid Description of the product",
                "Валидное описание товара",
                new BigDecimal("100.50"),
                ProductAvailability.IN_STOCK,
                10,
                new BigDecimal("80.00"),
                "Manufacturer Name",
                "Subcategory",
                "Подкатегория",
                "кг",
                1,
                List.of(new ProductImageDTO("img-1", true, 1, "alt")),
                Map.of("Color", "Red"),
                Map.of("Цвет", "Красный"),
                ProductType.SIMPLE,
                null,
                false,
                null,
                null,
                null,
                null
        );
    }

    private ProductResponseDTO createSampleProductResponseDTO(Long id) {
        return new ProductResponseDTO(
                id,
                "Valid Product Name",
                "valid-product-name",
                ProductCategory.NUTS,
                "Valid Description",
                new BigDecimal("100.50"),
                new BigDecimal("90.45"),
                10,
                BigDecimal.valueOf(4.5),
                10L,
                ProductAvailability.IN_STOCK,
                "Manufacturer",
                "Subcategory",
                "кг",
                1,
                false,
                ProductType.SIMPLE.name(),
                null,
                false,
                List.of(),
                List.of(),
                null,
                null,
                null,
                List.of(new ProductImageResponseDTO("img-1", "http://url", true, 1, "alt")),
                Map.of("Color", "Red")
        );
    }

    private ProductAdminResponseDTO createSampleProductAdminResponseDTO(Long id) {
        return new ProductAdminResponseDTO(
                id,
                "Valid Product Name",
                "Валидное имя товара",
                ProductCategory.NUTS,
                "Valid Description",
                "Валидное описание",
                new BigDecimal("100.50"),
                new BigDecimal("90.45"),
                new BigDecimal("80.00"),
                10,
                BigDecimal.valueOf(4.5),
                10L,
                ProductAvailability.IN_STOCK,
                "Manufacturer",
                "Subcategory",
                "Подкатегория",
                "кг",
                1,
                5L,
                ProductType.SIMPLE.name(),
                null,
                false,
                List.of(),
                List.of(),
                null,
                null,
                null,
                OffsetDateTime.now(),
                List.of(new ProductImageResponseDTO("img-1", "http://url", true, 1, "alt")),
                Map.of("Color", "Red"),
                Map.of("Цвет", "Красный")
        );
    }
}
