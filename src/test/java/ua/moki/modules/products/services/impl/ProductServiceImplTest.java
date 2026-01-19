package ua.moki.modules.products.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductImageDTO;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class ProductServiceImplTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createProduct_shouldPersistProductToDatabase() {

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "Кеш'ю смажений",
                ProductCategory.NUTS,
                "Дуже смачні горіхи",
                BigDecimal.valueOf(250.00),
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.valueOf(180.00),
                "Moki Nature",
                "Горіхи",
                "кг",
                1,
                List.of(new ProductImageDTO("photo_123", true, 1, "")),
                Map.of("country", "Vietnam")
        );

        ProductResponseDTO responseDTO = productService.createProduct(requestDTO);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isNotNull();
        assertThat(responseDTO.name()).isEqualTo(requestDTO.name());

        Product savedProduct = productRepository.findById(responseDTO.id()).orElseThrow();

        assertThat(savedProduct.getName()).isEqualTo("Кеш'ю смажений");

        assertThat(savedProduct.getRating()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(savedProduct.getCreationTime()).isNotNull();

        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
        assertThat(savedProduct.getInitOfMeasure()).isEqualTo("кг");
    }

    @Test
    void shouldUpdateBasicProductFields() {

        Product product = new Product();
        product.setName("Old name");
        product.setDescription("Description");
        product.setDiscount(0);
        product.setManufacturerOfTheProduct("Manufacturer");
        product.setPurchasePrice(BigDecimal.valueOf(50.00));
        product.setSubcategory("Subcategory");
        product.setInitOfMeasure("kg");
        product.setValueOfInitOfMeasure(1);
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setProductCategory(ProductCategory.NUTS);

        ProductImage image = new ProductImage();
        image.setImageId("old_photo_id");
        image.setMain(true);
        image.setSortOrder(1);
        image.setProduct(product);
        product.syncImages(List.of(image));

        productRepository.save(product);
        Long existingProductId = product.getId();

        ProductRequestDTO updateRequest = new ProductRequestDTO(
                "New name",
                ProductCategory.DRY_FRUITS,
                "Description",
                BigDecimal.valueOf(100.00),
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.valueOf(50.00),
                "Manufacturer",
                "Subcategory",
                "kg",
                1,
                null,
                Map.of("country", "Ukraine")
        );

        ProductResponseDTO response = productService.updateProduct(existingProductId, updateRequest);

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(100.00));

        Product updatedProduct = productRepository.findById(existingProductId).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("New name");
        assertThat(updatedProduct.getProductCategory()).isEqualTo(ProductCategory.DRY_FRUITS);
        assertThat(updatedProduct.getAvailability()).isEqualTo(ProductAvailability.IN_STOCK);
    }

    @Test
    @DisplayName("updateProduct correctly updates the photo list (Smart Merge)")
    void shouldUpdateImagesListCorrectly() {

        Product product = new Product();
        product.setName("Old name");
        product.setDescription("Description");
        product.setDiscount(0);
        product.setManufacturerOfTheProduct("Manufacturer");
        product.setPurchasePrice(BigDecimal.valueOf(50.00));
        product.setSubcategory("Subcategory");
        product.setInitOfMeasure("kg");
        product.setValueOfInitOfMeasure(1);
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setProductCategory(ProductCategory.NUTS);

        ProductImage image = new ProductImage();
        image.setImageId("old_photo_id");
        image.setMain(true);
        image.setSortOrder(1);
        image.setProduct(product);
        product.syncImages(List.of(image));

        productRepository.save(product);
        Long existingProductId = product.getId();

        ProductImageDTO newImage = new ProductImageDTO("new_photo_id", true, 2, "New Alt");

        ProductRequestDTO updateRequest = new ProductRequestDTO(
                "Стара назва",
                ProductCategory.NUTS,
                "Опис",
                BigDecimal.valueOf(100.00),
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.valueOf(50.00),
                "Brand",
                "Unit",
                "kg",
                1,
                List.of(newImage),
                Map.of()
        );

        productService.updateProduct(existingProductId, updateRequest);

        Product updatedProduct = productRepository.findById(existingProductId).orElseThrow();
        assertThat(updatedProduct.getImages()).hasSize(1); // Має залишитися тільки одне
        assertThat(updatedProduct.getImages().getFirst().getImageId()).isEqualTo("new_photo_id");

        boolean oldPhotoExists = updatedProduct.getImages().stream()
                .anyMatch(img -> img.getImageId().equals("old_photo_id"));

        assertThat(oldPhotoExists).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {

        Long nonExistentId = 9999L;
        ProductRequestDTO request = new ProductRequestDTO(
                "Name", ProductCategory.NUTS, "Desc", BigDecimal.TEN,
                ProductAvailability.IN_STOCK, 0, BigDecimal.ONE, "B", "U", "k", 1, List.of(), Map.of()
        );

        assertThatThrownBy(() -> productService.updateProduct(nonExistentId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void findById_shouldReturnProduct_whenItExists() {
        Product product = new Product();
        product.setName("Тестовий Продукт");
        product.setProductCategory(ProductCategory.SNACKS_AND_CHIPS);
        product.setDescription("Опис для тесту пошуку");
        product.setPrice(BigDecimal.valueOf(50.00));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setDiscount(0);
        product.setPurchasePrice(BigDecimal.valueOf(30.00));
        product.setManufacturerOfTheProduct("Test Factory");
        product.setSubcategory("Snacks");
        product.setInitOfMeasure("шт");
        product.setValueOfInitOfMeasure(1);
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());

        Product savedProduct = productRepository.save(product);

        Product foundProduct = productService.findById(savedProduct.getId());

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(savedProduct.getId());
        assertThat(foundProduct.getName()).isEqualTo("Тестовий Продукт");
    }

    @Test
    void findById_shouldThrowException_whenIdDoesNotExist() {

        Long nonExistentId = 999999L;

        assertThatThrownBy(() -> productService.findById(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Product not found with id: " + nonExistentId);
    }

//    @Test
//    @DisplayName("deleteProduct виконує HARD delete (видаляє з БД), якщо у продукта немає продажів")
//    void deleteProduct_shouldHardDelete_whenNoSales() {
//
//        Product savedProduct = createBaseProduct("Old name");
//
//        System.out.println(savedProduct.getSalesCount());
//        productService.deleteProduct(savedProduct.getId());
//
//        Optional<Product> deletedProduct = productRepository.findById(savedProduct.getId());
//        assertThat(deletedProduct).isEmpty(); // Має бути видалено повністю
//
//        // Перевіряємо, що викликався сервіс видалення картинок (якщо він розкоментований у коді)
//        // verify(fileStorageService, times(1)).deleteFiles(anyList());
//    }

    @Test
    @DisplayName("deleteProduct performs a SOFT delete (archive) if the product has sales")
    void deleteProduct_shouldSoftDelete_whenProductHasSales() {
        // Given
        Product product = createBaseProduct("Old name");

        // Симулюємо наявність продажів.
        // Варіант А: Якщо ви додали поле salesCount в Entity
         product.setSalesCount(5L);

        // Варіант Б: Якщо ви використовуєте orderItemRepository.existsByProductId(id)
        // Вам потрібно створити та зберегти Order/OrderItem в БД перед викликом тесту

        Product savedProduct = productRepository.save(product);

        productService.deleteProduct(savedProduct.getId());

        Optional<Product> archivedProduct = productRepository.findById(savedProduct.getId());

        assertThat(archivedProduct).isPresent();

        assertThat(archivedProduct.get().getAvailability()).isEqualTo(ProductAvailability.ARCHIVED);

        // Переконуємось, що файли НЕ видалялись для архівованого товару
        // verify(fileStorageService, times(0)).deleteFiles(anyList());
    }

    @Test
    @DisplayName("getAllProducts returns the products page and respects pagination")
    void getAllProducts_shouldReturnPagedProducts() {

        for (int i = 1; i <= 5; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Discount");
            product.setPurchasePrice(BigDecimal.valueOf(100 + i));
            product.setDiscount(0);
            product.setProductCategory(ProductCategory.SNACKS_AND_CHIPS);
            product.setPrice(BigDecimal.valueOf(100 + i));
            product.setAvailability(ProductAvailability.IN_STOCK);
            product.setInitOfMeasure("шт");
            product.setRating(BigDecimal.ZERO);
            product.setCreationTime(OffsetDateTime.now());
            product.setDescription("Desc " + i);
            product.setManufacturerOfTheProduct("Factory " + i);
            product.setSubcategory("Sub " + i);
            product.setValueOfInitOfMeasure(1);

            productRepository.save(product);
        }

        int page = 0;
        int size = 2;
        Page<ProductResponseDTO> resultPage = productService.getAllProducts(page, size);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(5);
        assertThat(resultPage.getTotalPages()).isEqualTo(3);
        assertThat(resultPage.getContent()).hasSize(2);

        Page<ProductResponseDTO> lastPage = productService.getAllProducts(2, size);

        assertThat(lastPage.getContent()).hasSize(1);
        assertThat(lastPage.getContent().get(0).name()).isEqualTo("Product 5");
    }

    @Test
    @DisplayName("getAllProducts returns an empty page if there are no records in the database")
    void getAllProducts_shouldReturnEmptyPage_whenDbIsEmpty() {

        Page<ProductResponseDTO> resultPage = productService.getAllProducts(0, 10);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).isEmpty();
        assertThat(resultPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAllProductByCategory returns only products of the specified category")
    void getAllProductByCategory_shouldReturnProductsOfSpecificCategory() {

        createAndSaveProduct("Nuts 1", ProductCategory.NUTS);
        createAndSaveProduct("Nuts 2", ProductCategory.NUTS);
        createAndSaveProduct("Nuts 3", ProductCategory.NUTS);
        createAndSaveProduct("Tea 1", ProductCategory.TEA);
        createAndSaveProduct("Coffee 1", ProductCategory.COFFEE);

        Page<ProductResponseDTO> result = productService.getAllProductByCategory(ProductCategory.NUTS, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .hasSize(3)
                .allMatch(dto -> dto.productCategory() == ProductCategory.NUTS);
    }

    @Test
    @DisplayName("getAllProductByCategory works correctly with pagination")
    void getAllProductByCategory_shouldRespectPagination() {

        for (int i = 1; i <= 5; i++) {
            createAndSaveProduct("Tea " + i, ProductCategory.TEA);
        }

        Page<ProductResponseDTO> firstPage = productService.getAllProductByCategory(ProductCategory.TEA, 0, 2);

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);

        Page<ProductResponseDTO> secondPage = productService.getAllProductByCategory(ProductCategory.TEA, 1, 2);
        assertThat(secondPage.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("getAllProductByCategory returns an empty page if the category is empty")
    void getAllProductByCategory_shouldReturnEmpty_whenNoProductsInCategory() {

        createAndSaveProduct("Candy", ProductCategory.CANDIES);

        Page<ProductResponseDTO> result = productService.getAllProductByCategory(ProductCategory.SPICES, 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void getNewProducts_shouldReturnSortedByCreationTimeDesc() {

        Product oldProduct = createBaseProduct("Old Product");
        oldProduct.setCreationTime(OffsetDateTime.now().minusDays(10));
        productRepository.save(oldProduct);

        Product midProduct = createBaseProduct("Mid Product");
        midProduct.setCreationTime(OffsetDateTime.now().minusDays(5));
        productRepository.save(midProduct);

        Product newProduct = createBaseProduct("New Product");
        newProduct.setCreationTime(OffsetDateTime.now().minusHours(1));
        productRepository.save(newProduct);

        Page<ProductResponseDTO> result = productService.getNewProducts(0, 10);

        assertThat(result.getContent()).hasSize(3);

        assertThat(result.getContent().get(0).name()).isEqualTo("New Product");
        assertThat(result.getContent().get(1).name()).isEqualTo("Mid Product");
        assertThat(result.getContent().get(2).name()).isEqualTo("Old Product");
    }

    @Test
    void getNewProducts_shouldRespectPagination() {

        for (int i = 0; i < 5; i++) {
            Product p = createBaseProduct("Product " + i);
            p.setCreationTime(OffsetDateTime.now().minusMinutes(10 * i));
            productRepository.save(p);
        }

        Page<ProductResponseDTO> firstPage = productService.getNewProducts(0, 2);

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent().get(0).name()).isEqualTo("Product 4");
        assertThat(firstPage.getContent().get(1).name()).isEqualTo("Product 3");

        Page<ProductResponseDTO> secondPage = productService.getNewProducts(1, 2);

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent().get(0).name()).isEqualTo("Product 2");
    }

    @Test
    @DisplayName("getProductsWithDiscount returns only products with a discount (> 0) and sorts them by newness")
    void getProductsWithDiscount_shouldReturnOnlyDiscounted_SortedByDate() {

        Product newDiscounted = createBaseProduct("New Discounted");
        newDiscounted.setDiscount(10);
        newDiscounted.setCreationTime(OffsetDateTime.now());
        productRepository.save(newDiscounted);

        Product oldDiscounted = createBaseProduct("Old Discounted");
        oldDiscounted.setDiscount(5);
        oldDiscounted.setCreationTime(OffsetDateTime.now().minusDays(5));
        productRepository.save(oldDiscounted);

        Product noDiscount = createBaseProduct("No Discount");
        noDiscount.setDiscount(0);
        noDiscount.setCreationTime(OffsetDateTime.now());
        productRepository.save(noDiscount);

        Page<ProductResponseDTO> result = productService.getProductsWithDiscount(0, 10);

        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent().get(0).name()).isEqualTo("New Discounted");
        assertThat(result.getContent().get(1).name()).isEqualTo("Old Discounted");

        boolean containsNoDiscount = result.getContent().stream()
                .anyMatch(p -> p.name().equals("No Discount"));
        assertThat(containsNoDiscount).isFalse();
    }

    @Test
    @DisplayName("getProductsWithDiscount works correctly with pagination")
    void getProductsWithDiscount_shouldRespectPagination() {

        for (int i = 0; i < 5; i++) {
            Product product = createBaseProduct("Discount Product " + i);
            product.setDiscount(10 + i);
            product.setCreationTime(OffsetDateTime.now().minusMinutes(i));
            productRepository.save(product);
        }

        Page<ProductResponseDTO> firstPage = productService.getProductsWithDiscount(0, 2);

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getContent().get(0).name()).isEqualTo("Discount Product 0");

        Page<ProductResponseDTO> secondPage = productService.getProductsWithDiscount(1, 2);

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent().get(0).name()).isEqualTo("Discount Product 2");
    }

    @Test
    @DisplayName("get Bestsellers returns products sorted by sales Count DESC")
    void getBestsellers_shouldReturnSortedBySalesCountDesc() {

        Product lowSales = createBaseProduct("Low Sales Product");
        lowSales.setSalesCount(5L);
        productRepository.save(lowSales);

        Product highSales = createBaseProduct("Top Seller");
        highSales.setSalesCount(1000L);
        productRepository.save(highSales);

        Product midSales = createBaseProduct("Mid Sales Product");
        midSales.setSalesCount(500L);
        productRepository.save(midSales);

        Page<ProductResponseDTO> result = productService.getBestsellers(0, 10);

        assertThat(result.getContent()).hasSize(3);

        assertThat(result.getContent().get(0).name()).isEqualTo("Top Seller");
        assertThat(result.getContent().get(1).name()).isEqualTo("Mid Sales Product");
        assertThat(result.getContent().get(2).name()).isEqualTo("Low Sales Product");
    }

    @Test
    @DisplayName("getBestsellers handles pagination correctly")
    void getBestsellers_shouldRespectPagination() {

        for (int i = 1; i <= 5; i++) {
            Product p = createBaseProduct("Product Sales " + i);
            p.setSalesCount(i * 10L); // 10, 20, 30, 40, 50
            productRepository.save(p);
        }

        Page<ProductResponseDTO> topPage = productService.getBestsellers(0, 2);

        assertThat(topPage.getContent()).hasSize(2);
        assertThat(topPage.getContent().get(0).name()).isEqualTo("Product Sales 5"); // sales = 50
        assertThat(topPage.getContent().get(1).name()).isEqualTo("Product Sales 4"); // sales = 40

        Page<ProductResponseDTO> secondPage = productService.getBestsellers(1, 2);

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent().get(0).name()).isEqualTo("Product Sales 3"); // sales = 30
    }

    private void createAndSaveProduct(String name, ProductCategory category) {
        Product product = new Product();
        product.setName(name);
        product.setProductCategory(category);
        product.setDescription("Test product");
        product.setPurchasePrice(BigDecimal.valueOf(100));
        product.setDiscount(0);
        product.setProductCategory(category);
        product.setPrice(BigDecimal.valueOf(100));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setInitOfMeasure("шт");
        product.setValueOfInitOfMeasure(1);
        product.setManufacturerOfTheProduct("Test Factory");
        product.setSubcategory("General");
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());

        productRepository.save(product);
    }


    private Product createBaseProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description");
        product.setDiscount(0);
        product.setManufacturerOfTheProduct("Manufacturer");
        product.setPurchasePrice(BigDecimal.valueOf(50.00));
        product.setSubcategory("Subcategory");
        product.setInitOfMeasure("kg");
        product.setValueOfInitOfMeasure(1);
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setProductCategory(ProductCategory.NUTS);

        ProductImage image = new ProductImage();
        image.setImageId("old_photo_id");
        image.setMain(true);
        image.setSortOrder(1);
        image.setProduct(product);
        product.syncImages(List.of(image));

        productRepository.save(product);
        return product;
    }

}

