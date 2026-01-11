package ua.moki.modules.products.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.modules.products.dtos.ProductImageDTO;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.services.ProductService;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createProduct_shouldReturnCreated_whenRequestIsValid() throws Exception {

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "Test Product",
                ProductCategory.NUTS,
                "Delicious nuts description",
                BigDecimal.valueOf(100.00),
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.valueOf(80.00),
                "Best Manufacturer",
                "Almonds",
                "кг",
                1,
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("origin", "USA")
        );

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                1L,
                "Test Product",
                ProductCategory.NUTS,
                "Delicious nuts description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0), // rating
                ProductAvailability.IN_STOCK,
                0,
                "Best Manufacturer",
                "Almonds",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("origin", "USA")
        );

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/products/1"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.price", is(100.0)));

        verify(productService).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    void createProduct_shouldReturnBadRequest_whenValidationFails() throws Exception {

        ProductRequestDTO invalidRequestDTO = new ProductRequestDTO(
                "A",
                ProductCategory.NUTS,
                "Short",
                BigDecimal.valueOf(-10.00),
                ProductAvailability.IN_STOCK,
                99,
                BigDecimal.valueOf(100.00),
                "",
                "Sub",
                "invalid_unit",
                0,
                null,
                null
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDTO)))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("PUT /product/{id} - successfully updates the product and returns 200 OK")
    void updateProduct_shouldReturnOk_whenRequestIsValid() throws Exception {

        Long productId = 1L;

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "Оновлений Горіх",
                ProductCategory.NUTS,
                "Оновлений опис продукту",
                BigDecimal.valueOf(200.00),
                ProductAvailability.IN_STOCK,
                5,
                BigDecimal.valueOf(120.00),
                "New Manufacturer",
                "Almonds",
                "кг",
                1,
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("origin", "USA")
        );

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                productId,
                "Оновлений Горіх",
                ProductCategory.NUTS,
                "Оновлений опис продукту",
                BigDecimal.valueOf(200.00),
                BigDecimal.valueOf(4.8),
                ProductAvailability.IN_STOCK,
                5,
                "New Manufacturer",
                "Almonds",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("origin", "USA")
        );

        when(productService.updateProduct(eq(productId), any(ProductRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(productId.intValue())))
                .andExpect(jsonPath("$.name", is("Оновлений Горіх")))
                .andExpect(jsonPath("$.price", is(200.0)))
                .andExpect(jsonPath("$.discount", is(5)));

        verify(productService).updateProduct(eq(productId), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /product/{id} - returns 400 Bad Request for invalid data")
    void updateProduct_shouldReturnBadRequest_whenValidationFails() throws Exception {

        Long productId = 1L;

        ProductRequestDTO invalidRequestDTO = new ProductRequestDTO(
                "",
                ProductCategory.NUTS,
                "Short",
                BigDecimal.valueOf(-1.00),
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.valueOf(100.00),
                "Manuf",
                "Sub",
                "kg",
                1,
                null,
                null
        );

        mockMvc.perform(put("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @DisplayName("PUT /product/{id} - return 404 when entity not found")
    void updateProduct_shouldReturnNotFound_whenEntityNotFound() throws Exception {

        Long productId = 1L;

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "Оновлений Горіх",
                ProductCategory.NUTS,
                "Оновлений опис продукту",
                BigDecimal.valueOf(200.00),
                ProductAvailability.IN_STOCK,
                5,
                BigDecimal.valueOf(120.00),
                "New Manufacturer",
                "Almonds",
                "кг",
                1,
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("origin", "USA")
        );

        when(productService.updateProduct(eq(productId), any(ProductRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Product not found"));

        mockMvc.perform(put("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());

        verify(productService).updateProduct(eq(productId), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("GET /products/{id} - returns 200 OK and DTO if product found")
    void getProductById_shouldReturnProduct_whenExists() throws Exception {
        // Given
        Long productId = 1L;

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                productId,
                "Test Product",
                ProductCategory.NUTS,
                "Description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0),
                ProductAvailability.IN_STOCK,
                0,
                "Test Manufacturer",
                "Subcategory",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("1", true, 1, "")),
                Map.of("origin", "Ukraine")
        );

        when(productService.getProductById(productId)).thenReturn(responseDTO);

        mockMvc.perform(get("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(productId.intValue())))
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.price", is(100.0)));

        verify(productService).getProductById(productId);
    }

    @Test
    @DisplayName("GET /products/{id} - returns 404 Not Found if the product is missing")
    void getProductById_shouldReturnNotFound_whenIdDoesNotExist() throws Exception {

        Long nonExistentId = 999L;

        when(productService.getProductById(nonExistentId))
                .thenThrow(new EntityNotFoundException("Product not found with id: " + nonExistentId));

        mockMvc.perform(get("/products/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()) // Перевіряємо статус 404
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("Product not found with id: 999")));

        verify(productService).getProductById(nonExistentId);
    }

    @Test
    @DisplayName("GET /products - повертає сторінку з продуктами (200 OK)")
    void getAllProducts_shouldReturnPageOfProducts() throws Exception {

        int page = 1;
        int size = 10;

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                1L,
                "Test Product",
                ProductCategory.NUTS,
                "Description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0),
                ProductAvailability.IN_STOCK,
                0,
                "Test Manufacturer",
                "Subcategory",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("1", true, 1, "")),
                Map.of("origin", "Ukraine")
        );

        PageImpl<ProductResponseDTO> productPage = new PageImpl<>(List.of(responseDTO));

        when(productService.getAllProducts(page, size)).thenReturn(productPage);

        mockMvc.perform(get("/products")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Test Product")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(productService).getAllProducts(page, size);
    }

    @Test
    @DisplayName("GET /products - повертає порожню сторінку, якщо продуктів немає")
    void getAllProducts_shouldReturnEmptyPage_whenNoProductsFound() throws Exception {

        int page = 0;
        int size = 10;

        when(productService.getAllProducts(page, size)).thenReturn(Page.empty());

        mockMvc.perform(get("/products")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(productService).getAllProducts(page, size);
    }

    @Test
    @DisplayName("GET /products - повертає 400 Bad Request, якщо не передані параметри page/size")
    void getAllProducts_shouldReturnBadRequest_whenParamsMissing() throws Exception {

        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getAllProducts(anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /products/new - returns the new products page (200 OK)")
    void getNewProducts_shouldReturnPageOfProducts() throws Exception {

        int page = 0;
        int size = 10;

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                1L,
                "New Arrival Product",
                ProductCategory.NUTS,
                "Description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0),
                ProductAvailability.IN_STOCK,
                0,
                "Test Manufacturer",
                "Subcategory",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("1", true, 1, "")),
                Map.of("origin", "Ukraine")
        );

        PageImpl<ProductResponseDTO> pageResult = new PageImpl<>(List.of(responseDTO));

        when(productService.getNewProducts(page, size)).thenReturn(pageResult);

        mockMvc.perform(get("/products/new")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("New Arrival Product")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(productService).getNewProducts(page, size);
    }

    @Test
    @DisplayName("GET /products/new - returns a blank page if there are no new products")
    void getNewProducts_shouldReturnEmptyPage() throws Exception {

        int page = 0;
        int size = 10;

        when(productService.getNewProducts(page, size)).thenReturn(Page.empty());

        mockMvc.perform(get("/products/new")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(productService).getNewProducts(page, size);
    }

    @Test
    @DisplayName("GET /products/new - returns 400 Bad Request if no parameters are provided")
    void getNewProducts_shouldReturnBadRequest_whenParamsMissing() throws Exception {

        mockMvc.perform(get("/products/new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getNewProducts(anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /products/category/{category} - returns products of the selected category (200 OK)")
    void getProductsByCategory_shouldReturnPageOfProducts() throws Exception {

        ProductCategory category = ProductCategory.NUTS;
        int page = 0;
        int size = 10;

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                1L,
                "Test Product",
                ProductCategory.NUTS,
                "Description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0),
                ProductAvailability.IN_STOCK,
                0,
                "Test Manufacturer",
                "Subcategory",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("1", true, 1, "")),
                Map.of("origin", "Ukraine")
        );

        PageImpl<ProductResponseDTO> pageResult = new PageImpl<>(List.of(responseDTO));

        when(productService.getAllProductByCategory(category, page, size)).thenReturn(pageResult);

        mockMvc.perform(get("/products/category/{category}", category)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productCategory", is(category.name())))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(productService).getAllProductByCategory(category, page, size);
    }

    @Test
    @DisplayName("GET /products/category/{category} - returns 400 Bad Request if the category does not exist")
    void getProductsByCategory_shouldReturnBadRequest_whenCategoryIsInvalid() throws Exception {

        String invalidCategory = "INVALID_CATEGORY";

        mockMvc.perform(get("/products/category/{category}", invalidCategory)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getAllProductByCategory(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /products/category/{category} - returns a blank page if there are no products")
    void getProductsByCategory_shouldReturnEmptyPage() throws Exception {

        ProductCategory category = ProductCategory.TEA;
        when(productService.getAllProductByCategory(category, 0, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/products/category/{category}", category)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /products/discount - returns a page of discounted products (200 OK)")
    void getProductsWithDiscount_shouldReturnPageOfProducts() throws Exception {

        int page = 0;
        int size = 10;

        ProductResponseDTO responseDTO = new ProductResponseDTO(
                1L,
                "Test Product",
                ProductCategory.NUTS,
                "Description",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.0),
                ProductAvailability.IN_STOCK,
                20,
                "Test Manufacturer",
                "Subcategory",
                "кг",
                1,
                0L,
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("1", true, 1, "")),
                Map.of("origin", "Ukraine")
        );

        PageImpl<ProductResponseDTO> pageResult = new PageImpl<>(List.of(responseDTO));

        when(productService.getProductsWithDiscount(page, size)).thenReturn(pageResult);

        mockMvc.perform(get("/products/discount")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Test Product")))
                .andExpect(jsonPath("$.content[0].discount", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(productService).getProductsWithDiscount(page, size);
    }

    @Test
    @DisplayName("GET /products/discount - returns a blank page if there are no promotional products")
    void getProductsWithDiscount_shouldReturnEmptyPage() throws Exception {

        when(productService.getProductsWithDiscount(0, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/products/discount")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(productService).getProductsWithDiscount(0, 10);
    }

    @Test
    @DisplayName("GET /products/discount - returns 400 Bad Request with negative parameters")
    void getProductsWithDiscount_shouldReturnBadRequest_whenParamsInvalid() throws Exception {

        mockMvc.perform(get("/products/discount")
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProductsWithDiscount(anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /products/bestsellers - повертає сторінку бестселерів (200 OK)")
    void getBestsellers_shouldReturnPageOfProducts() throws Exception {

        int page = 0;
        int size = 10;

        ProductResponseDTO dto = new ProductResponseDTO(
                50L,
                "Top Seller Product",
                ProductCategory.SNACKS_AND_CHIPS,
                "Most popular chips",
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(4.9),
                ProductAvailability.IN_STOCK,
                0,
                "Chip Factory",
                "Chips",
                "pack",
                1,
                1000L, // salesCount
                OffsetDateTime.now(),
                List.of(new ProductImageDTO("img_best", true, 0, "url")),
                Map.of("flavor", "Paprika")
        );

        PageImpl<ProductResponseDTO> pageResult = new PageImpl<>(List.of(dto));

        when(productService.getBestsellers(page, size)).thenReturn(pageResult);

        mockMvc.perform(get("/products/bestsellers")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Top Seller Product")))
                .andExpect(jsonPath("$.content[0].salesCount", is(1000))) // Перевіряємо поле продажів
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(productService).getBestsellers(page, size);
    }

    @Test
    @DisplayName("GET /products/bestsellers - повертає пусту сторінку, якщо бестселерів немає")
    void getBestsellers_shouldReturnEmptyPage() throws Exception {

        when(productService.getBestsellers(0, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/products/bestsellers")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(productService).getBestsellers(0, 10);
    }

    @Test
    @DisplayName("GET /products/bestsellers - повертає 400 Bad Request при некоректних параметрах")
    void getBestsellers_shouldReturnBadRequest_whenParamsInvalid() throws Exception {

        mockMvc.perform(get("/products/bestsellers")
                        .param("page", "-1") // Невалідний page
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getBestsellers(anyInt(), anyInt());
    }

}
