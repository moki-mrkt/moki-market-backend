package ua.moki.modules.products.services.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ua.moki.BaseIntegrationTest;
import ua.moki.infrastructure.storage.service.FileStorageService;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.*;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ProductServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("createProduct successfully saves the product, images, characteristics, and calculates the rating")
    void createProduct_shouldPersistProductToDatabase() {

        List<ProductImageDTO> imageDTOs = List.of(
                new ProductImageDTO("main-image-key-123", true, 1, "Main alt"),
                new ProductImageDTO("sec-image-key-456", false, 2, "Sec alt")
        );

        Map<String, String> characteristics = Map.of(
                "Processor", "M3 Max",
                "RAM", "64GB",
                "Color", "Space Black"
        );

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "MacBook Pro 16",
                "Макбук Про 16",
                ProductCategory.NUTS,
                "Super powerful laptop",
                "Супер мощный ноутбук",
                new BigDecimal("150000.00"),
                ProductAvailability.IN_STOCK,
                10,
                new BigDecimal("120000.00"),
                "Apple",
                "Laptops",
                "Ноутбуки",
                "шт",
                1,
                imageDTOs,
                characteristics,
                Map.of("Процессор", "M3 Max"),
                ProductType.SIMPLE,
                null,
                false,
                null,
                null,
                null,
                null
        );

        ProductResponseDTO response = productService.createProduct(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("MacBook Pro 16");
        assertThat(response.slug()).isEqualTo("macbook-pro-16");
        assertThat(response.price()).isEqualByComparingTo("150000.00");
        assertThat(response.rating()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.characteristics())
                .hasSize(3)
                .containsEntry("Processor", "M3 Max");

        assertThat(response.images()).hasSize(2);
        assertThat(response.images().getFirst().imageUrl())
                .isEqualTo("http://localhost:9000/moki-images/main-image-key-123");

        Product savedProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedProduct.getName()).isEqualTo("MacBook Pro 16");
        assertThat(savedProduct.getNameRu()).isEqualTo("Макбук Про 16");
        assertThat(savedProduct.getPurchasePrice()).isEqualByComparingTo("120000.00");
        assertThat(savedProduct.getCreationTime()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));

        assertThat(savedProduct.getCharacteristics()).containsKey("RAM");

        assertThat(savedProduct.getImages()).hasSize(2);

        ProductImage firstImage = savedProduct.getImages().getFirst();
        assertThat(firstImage.getImageId()).isEqualTo("main-image-key-123");
        assertThat(firstImage.isMain()).isTrue();
        assertThat(firstImage.getProduct()).isEqualTo(savedProduct);
    }

    @Test
    @DisplayName("createProduct saves WEIGHT_BASED product with weight options")
    void createProduct_shouldSaveWeightBasedProductWithOptions() {
        List<ProductWeightOptionDTO> weightOptions = List.of(
                new ProductWeightOptionDTO(null, 100, new BigDecimal("55.00"), false, true),
                new ProductWeightOptionDTO(null, 500, new BigDecimal("250.00"), true, true)
        );

        ProductRequestDTO requestDTO = new ProductRequestDTO(
                "Мигдаль смажений",
                "Миндаль жареный",
                ProductCategory.NUTS,
                "Смачний мигдаль",
                "Вкусный миндаль",
                new BigDecimal("500.00"),
                ProductAvailability.IN_STOCK,
                0,
                new BigDecimal("380.00"),
                "Moki Nuts",
                "Мигдаль",
                "Миндаль",
                "кг",
                1,
                List.of(),
                Map.of("Країна", "США"),
                Map.of("Страна", "США"),
                ProductType.WEIGHT_BASED,
                150,
                true,
                weightOptions,
                null,
                null,
                null
        );

        ProductResponseDTO response = productService.createProduct(requestDTO);

        assertThat(response.productType()).isEqualTo(ProductType.WEIGHT_BASED.name());
        assertThat(response.allowCustomWeight()).isTrue();
        assertThat(response.minCustomWeight()).isEqualTo(150);
        assertThat(response.weightOptions()).hasSize(2);
    }

    @Test
    @DisplayName("updateProduct updates the basic product fields")
    void shouldUpdateBasicProductFields() {
        Product existingProduct = createBaseProduct("Old Phone", new BigDecimal("100.00"));
        existingProduct.setRating(BigDecimal.valueOf(5));
        existingProduct.setReviewsCount(1L);
        existingProduct.setCharacteristics(new java.util.HashMap<>(Map.of("RAM", "4GB")));
        existingProduct = productRepository.save(existingProduct);

        ProductRequestDTO updateRequest = new ProductRequestDTO(
                "New Phone",
                "Новый телефон",
                ProductCategory.NUTS,
                "Updated Description",
                "Обновленное описание",
                new BigDecimal("150.00"),
                ProductAvailability.OUT_OF_STOCK,
                10,
                new BigDecimal("80.00"),
                "Samsung",
                "Smartphones",
                "Смартфоны",
                "шт",
                1,
                Collections.emptyList(),
                Map.of("RAM", "8GB", "Color", "Black"),
                Map.of("ОЗУ", "8ГБ"),
                ProductType.SIMPLE,
                null,
                false,
                null,
                null,
                null,
                null
        );

        ProductResponseDTO response = productService.updateProduct(existingProduct.getId(), updateRequest);

        assertThat(response.name()).isEqualTo("New Phone");
        assertThat(response.price()).isEqualByComparingTo("150.00");
        assertThat(response.availability()).isEqualTo(ProductAvailability.OUT_OF_STOCK);
        assertThat(response.characteristics()).containsEntry("RAM", "8GB").containsEntry("Color", "Black");

        Product updatedInDb = productRepository.findById(existingProduct.getId()).orElseThrow();
        assertThat(updatedInDb.getName()).isEqualTo("New Phone");
        assertThat(updatedInDb.getNameRu()).isEqualTo("Новый телефон");
        assertThat(updatedInDb.getDiscount()).isEqualTo(10);
        assertThat(updatedInDb.getManufacturerOfTheProduct()).isEqualTo("Samsung");
    }

    @Test
    @DisplayName("updateProduct properly synchronizes images (deletes old ones, adds new ones)")
    void shouldUpdateImagesListCorrectly() {

        Product product = createBaseProduct("Camera", new BigDecimal("500.00"));

        ProductImage img1 = new ProductImage();
        img1.setImageId("img_1");
        img1.setMain(true);
        img1.setSortOrder(1);
        img1.setProduct(product);

        ProductImage img2 = new ProductImage();
        img2.setImageId("img_2");
        img2.setMain(false);
        img2.setSortOrder(2);
        img2.setProduct(product);

        product.syncImages(List.of(img1, img2));
        product = productRepository.save(product);

        List<ProductImageDTO> newImagesDto = List.of(
                new ProductImageDTO("img_2", true, 1, "Alt 2"),
                new ProductImageDTO("img_3", false, 2, "Alt 3")
        );

        ProductRequestDTO updateRequest = new ProductRequestDTO(
                "Camera",
                "Камера",
                ProductCategory.NUTS,
                "Desc",
                "Описание",
                new BigDecimal("500.00"),
                ProductAvailability.IN_STOCK,
                0,
                new BigDecimal("400.00"),
                "Canon",
                "Photo",
                "Фото",
                "шт",
                1,
                newImagesDto,
                Collections.emptyMap(),
                Collections.emptyMap(),
                ProductType.SIMPLE,
                null,
                false,
                null,
                null,
                null,
                null
        );

        productService.updateProduct(product.getId(), updateRequest);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        List<ProductImage> currentImages = updatedProduct.getImages();

        assertThat(currentImages).hasSize(2);

        assertThat(currentImages)
                .extracting(ProductImage::getImageId)
                .containsExactlyInAnyOrder("img_2", "img_3")
                .doesNotContain("img_1");

        ProductImage updatedImg2 = currentImages.stream()
                .filter(img -> img.getImageId().equals("img_2"))
                .findFirst().orElseThrow();
        assertThat(updatedImg2.isMain()).isTrue();
    }

    @Test
    @DisplayName("updateProduct throws an error if the product does not exist")
    void shouldThrowExceptionWhenProductNotFound() {
        Long nonExistentId = 9999L;
        ProductRequestDTO request = new ProductRequestDTO(
                "Ghost",
                "Призрак",
                ProductCategory.SWEETS,
                "Desc",
                "Описание",
                BigDecimal.TEN,
                ProductAvailability.IN_STOCK,
                0,
                BigDecimal.ONE,
                "M",
                "S",
                "П",
                "г",
                1,
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

        assertThatThrownBy(() -> productService.updateProduct(nonExistentId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("deleteProduct archives an existing product in the database")
    void deleteProduct_shouldRemoveProductFromDb_whenProductExists() {
        Product savedProduct = productRepository.save(createBaseProduct("Product to Delete", BigDecimal.TEN));
        Long productId = savedProduct.getId();

        assertThat(productRepository.existsById(productId)).isTrue();

        productService.deleteProduct(productId);

        Product archivedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(archivedProduct.getAvailability()).isEqualTo(ProductAvailability.ARCHIVED);
    }

    @Test
    @DisplayName("deleteProduct throws EntityNotFoundException if product does not exist")
    void deleteProduct_shouldThrowException_whenProductDoesNotExist() {
        Long nonExistentId = 999999L;

        assertThatThrownBy(() -> productService.deleteProduct(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("findById returns a Product entity when it exists in the database")
    void findById_shouldReturnProduct_whenItExists() {
        Product product = createBaseProduct("Found Product", new BigDecimal("200.00"));
        product.setProductCategory(ProductCategory.SWEETS);
        product.setSubcategory("Chocolates");
        product.setSubcategoryRu("Шоколад");
        product.setPurchasePrice(new BigDecimal("150.00"));
        product.setManufacturerOfTheProduct("Roshen");
        product.setInitOfMeasure("г");
        product.setValueOfInitOfMeasure(100);

        Product savedProduct = productRepository.save(product);

        Product foundProduct = productService.findById(savedProduct.getId());

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(savedProduct.getId());
        assertThat(foundProduct.getName()).isEqualTo("Found Product");
        assertThat(foundProduct.getPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when ID does not exist")
    void findById_shouldThrowException_whenIdDoesNotExist() {
        Long nonExistentId = 888888L;

        assertThatThrownBy(() -> productService.findById(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("getProductById returns the full DTO with images and characteristics")
    void getProductById_shouldReturnDto_whenProductExists() {
        Product product = createBaseProduct("iPhone 15", new BigDecimal("40000.00"));
        product.setDescription("Apple smartphone");
        product.setDescriptionRu("Смартфон Apple");
        product.setRating(BigDecimal.valueOf(4.5));
        product.setDiscount(5);
        product.setPurchasePrice(new BigDecimal("35000.00"));
        product.setManufacturerOfTheProduct("Apple");
        product.setSubcategory("Phones");
        product.setSubcategoryRu("Телефоны");
        product.setCharacteristics(Map.of("Color", "Blue", "Memory", "128GB"));

        ProductImage image = new ProductImage();
        image.setImageId("iphone-img-key");
        image.setMain(true);
        image.setSortOrder(1);
        product.syncImages(List.of(image));

        Product savedProduct = productRepository.save(product);

        ProductResponseDTO result = productService.getProductById(savedProduct.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(savedProduct.getId());
        assertThat(result.name()).isEqualTo("iPhone 15");
        assertThat(result.price()).isEqualByComparingTo("40000.00");
        assertThat(result.rating()).isEqualTo(BigDecimal.valueOf(4.5));

        assertThat(result.characteristics())
                .hasSize(2)
                .containsEntry("Color", "Blue");

        assertThat(result.images()).hasSize(1);
        assertThat(result.images().getFirst().imageUrl())
                .isEqualTo("http://localhost:9000/moki-images/iphone-img-key");
    }

    @Test
    @DisplayName("getProductByIdForAdmin returns ProductAdminResponseDTO with RU fields and purchasePrice")
    void getProductByIdForAdmin_shouldReturnAdminDto_whenProductExists() {
        Product product = createBaseProduct("Матча Церемоніальна", new BigDecimal("650.00"));
        product.setNameRu("Матча Церемониальная");
        product.setPurchasePrice(new BigDecimal("450.00"));
        Product savedProduct = productRepository.save(product);

        ProductAdminResponseDTO adminDto = productService.getProductByIdForAdmin(savedProduct.getId());

        assertThat(adminDto).isNotNull();
        assertThat(adminDto.id()).isEqualTo(savedProduct.getId());
        assertThat(adminDto.name()).isEqualTo("Матча Церемоніальна");
        assertThat(adminDto.nameRu()).isEqualTo("Матча Церемониальная");
        assertThat(adminDto.purchasePrice()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("getProductBySlug returns ProductResponseDTO when slug exists")
    void getProductBySlug_shouldReturnProduct_whenSlugExists() {
        Product product = createBaseProduct("Matcha Tea", new BigDecimal("350.00"));
        product.setSlug("matcha-tea-unique");
        Product savedProduct = productRepository.save(product);

        ProductResponseDTO dto = productService.getProductBySlug("matcha-tea-unique");

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(savedProduct.getId());
        assertThat(dto.slug()).isEqualTo("matcha-tea-unique");
    }

    @Test
    @DisplayName("getProductBySlug throws EntityNotFoundException when slug does not exist")
    void getProductBySlug_shouldThrowException_whenSlugDoesNotExist() {
        assertThatThrownBy(() -> productService.getProductBySlug("non-existent-slug"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("getProductById throws EntityNotFoundException if product does not exist")
    void getProductById_shouldThrowException_whenProductDoesNotExist() {
        Long nonExistentId = 777777L;

        assertThatThrownBy(() -> productService.getProductById(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("getAllProducts returns the correct product page according to Pageable")
    void getAllProducts_shouldReturnPagedProducts() {
        createAndSaveProduct("Product A", new BigDecimal("100.00"));
        createAndSaveProduct("Product B", new BigDecimal("200.00"));
        createAndSaveProduct("Product C", new BigDecimal("300.00"));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

        Page<ProductAdminResponseDTO> result = productService.getAllProducts("", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent())
                .extracting(ProductAdminResponseDTO::name)
                .containsExactly("Product A", "Product B");
    }

    @Test
    @DisplayName("getAllProducts returns an empty page if the database is empty")
    void getAllProducts_shouldReturnEmptyPage_whenDbIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProductAdminResponseDTO> result = productService.getAllProducts("", pageable);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getNewProducts returns products sorted from newest to oldest")
    void getNewProducts_shouldReturnProductsSortedByDateDesc() {
        OffsetDateTime now = OffsetDateTime.now();

        createProductWithDate("Old Product", now.minusDays(5));
        createProductWithDate("Newest Product", now);
        createProductWithDate("Middle Product", now.minusDays(2));

        Page<ProductResponseDTO> result = productService.getNewProducts(0, 10);

        assertThat(result.getContent()).hasSize(3);

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactly("Newest Product", "Middle Product", "Old Product");
    }

    @Test
    @DisplayName("getNewProducts correctly handles pagination")
    void getNewProducts_shouldRespectPagination() {
        OffsetDateTime now = OffsetDateTime.now();

        createProductWithDate("Product 1 (Oldest)", now.minusHours(3));
        createProductWithDate("Product 2", now.minusHours(2));
        createProductWithDate("Product 3 (Newest)", now.minusHours(1));

        Page<ProductResponseDTO> page0 = productService.getNewProducts(0, 2);

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactly("Product 3 (Newest)", "Product 2");

        Page<ProductResponseDTO> page1 = productService.getNewProducts(1, 2);

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getContent().getFirst().name()).isEqualTo("Product 1 (Oldest)");
    }

    @Test
    @DisplayName("getAllProductByCategory returns ONLY products of the specified category, sorted from newest")
    void getAllProductByCategory_shouldReturnFilteredAndSortedProducts() {
        OffsetDateTime now = OffsetDateTime.now();

        createProductInCategory("Old Sweet", ProductCategory.SWEETS, now.minusDays(5));
        createProductInCategory("New Sweet", ProductCategory.SWEETS, now.minusDays(1));
        createProductInCategory("Grocery Item", ProductCategory.NUTS, now);

        Page<ProductResponseDTO> result = productService.getAllProductByCategory(ProductCategory.SWEETS, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::productCategory)
                .containsOnly(ProductCategory.SWEETS);

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactlyInAnyOrder("New Sweet", "Old Sweet");
    }

    @Test
    @DisplayName("getAllProductByCategory returns an empty page if there are no products in this category")
    void getAllProductByCategory_shouldReturnEmptyPage_whenNoMatches() {
        createProductInCategory("Some Sweet", ProductCategory.SWEETS, OffsetDateTime.now());

        Page<ProductResponseDTO> result = productService.getAllProductByCategory(ProductCategory.NUTS, 0, 10);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getProductsWithDiscount returns ONLY products with a discount > 0")
    void getProductsWithDiscount_shouldReturnOnlyDiscountedProducts() {
        OffsetDateTime now = OffsetDateTime.now();

        createProductWithDiscount("Old Sale Item", 10, now.minusDays(5));
        createProductWithDiscount("New Sale Item", 50, now.minusDays(1));
        createProductWithDiscount("Regular Item", 0, now);

        Page<ProductResponseDTO> result = productService.getProductsWithDiscount(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::name)
                .doesNotContain("Regular Item");

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactly("Old Sale Item", "New Sale Item");
    }

    @Test
    @DisplayName("getProductsWithDiscount returns an empty page if there are no discounted products")
    void getProductsWithDiscount_shouldReturnEmptyPage_whenNoDiscounts() {
        createProductWithDiscount("Regular 1", 0, OffsetDateTime.now());
        createProductWithDiscount("Regular 2", 0, OffsetDateTime.now());

        Page<ProductResponseDTO> result = productService.getProductsWithDiscount(0, 10);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getBestsellers returns products sorted by sales count (DESC)")
    void getBestsellers_shouldReturnProductsSortedBySalesDesc() {
        createProductWithSales("Mid Seller", 50L);
        createProductWithSales("Top Seller", 1000L);
        createProductWithSales("Low Seller", 5L);
        createProductWithSales("Zero Seller", 0L);

        Page<ProductResponseDTO> result = productService.getBestsellers(0, 10);

        assertThat(result.getContent()).hasSize(4);

        assertThat(result.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactly("Top Seller", "Mid Seller", "Low Seller", "Zero Seller");
    }

    @Test
    @DisplayName("getBestsellers correctly handles pagination (e.g. TOP-2)")
    void getBestsellers_shouldRespectPagination() {
        createProductWithSales("Rank 1", 300L);
        createProductWithSales("Rank 2", 200L);
        createProductWithSales("Rank 3", 100L);

        Page<ProductResponseDTO> top2 = productService.getBestsellers(0, 2);

        assertThat(top2.getContent()).hasSize(2);
        assertThat(top2.getTotalElements()).isEqualTo(3);

        assertThat(top2.getContent())
                .extracting(ProductResponseDTO::name)
                .containsExactly("Rank 1", "Rank 2");
    }

    @Test
    @DisplayName("searchProducts filters by category, price, and discount and returns facets")
    void searchProducts_shouldFilterAndReturnFacets() {
        Product p1 = createBaseProduct("Манго Royal", new BigDecimal("200.00"));
        p1.setProductCategory(ProductCategory.DRIED_FRUITS);
        p1.setSubcategory("Манго");
        p1.setSubcategoryRu("Манго");
        p1.setDiscount(10);
        productRepository.save(p1);

        Product p2 = createBaseProduct("Фініки Королівські", new BigDecimal("250.00"));
        p2.setProductCategory(ProductCategory.DRIED_FRUITS);
        p2.setSubcategory("Фініки");
        p2.setSubcategoryRu("Финики");
        p2.setDiscount(5);
        productRepository.save(p2);

        ProductSearchRequestDTO searchRequest = new ProductSearchRequestDTO(
                null,
                ProductCategory.DRIED_FRUITS,
                100.0,
                300.0,
                List.of("Манго"),
                true
        );

        SearchResponse response = productService.searchProducts(searchRequest, PageRequest.of(0, 12));

        assertThat(response).isNotNull();
        assertThat(response.products().getTotalElements()).isEqualTo(1);
        assertThat(response.products().getContent().getFirst().name()).isEqualTo("Манго Royal");
        assertThat(response.subcategories()).contains("Манго", "Фініки");
    }

    private Product createBaseProduct(String name, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setNameRu(name + " RU");
        p.setSlug("slug-" + UUID.randomUUID());
        p.setProductCategory(ProductCategory.NUTS);
        p.setDescription("Default desc");
        p.setDescriptionRu("Default desc RU");
        p.setPrice(price);
        p.setAvailability(ProductAvailability.IN_STOCK);
        p.setDiscount(0);
        p.setPurchasePrice(BigDecimal.ONE);
        p.setManufacturerOfTheProduct("Default Man");
        p.setSubcategory("Default Sub");
        p.setSubcategoryRu("Default Sub RU");
        p.setInitOfMeasure("шт");
        p.setValueOfInitOfMeasure(1);
        p.setProductType(ProductType.SIMPLE);
        p.setAllowCustomWeight(false);
        p.setCreationTime(OffsetDateTime.now());
        p.setRating(BigDecimal.ZERO);
        p.setReviewsCount(0L);
        return p;
    }

    private void createAndSaveProduct(String name, BigDecimal price) {
        productRepository.save(createBaseProduct(name, price));
    }

    private void createProductWithDate(String name, OffsetDateTime desiredDate) {
        Product p = createBaseProduct(name, BigDecimal.TEN);
        p = productRepository.saveAndFlush(p);

        entityManager.createNativeQuery("UPDATE products SET creation_time = ? WHERE id = ?")
                .setParameter(1, desiredDate)
                .setParameter(2, p.getId())
                .executeUpdate();

        entityManager.clear();
    }

    private void createProductInCategory(String name, ProductCategory category, OffsetDateTime creationTime) {
        Product p = createBaseProduct(name, BigDecimal.TEN);
        p.setProductCategory(category);
        p = productRepository.saveAndFlush(p);

        entityManager.createNativeQuery("UPDATE products SET creation_time = ? WHERE id = ?")
                .setParameter(1, creationTime)
                .setParameter(2, p.getId())
                .executeUpdate();

        entityManager.clear();
    }

    private void createProductWithDiscount(String name, int discount, OffsetDateTime creationTime) {
        Product p = createBaseProduct(name, new BigDecimal("100.00"));
        p.setDiscount(discount);
        p = productRepository.saveAndFlush(p);

        entityManager.createNativeQuery("UPDATE products SET creation_time = ? WHERE id = ?")
                .setParameter(1, creationTime)
                .setParameter(2, p.getId())
                .executeUpdate();

        entityManager.clear();
    }

    private void createProductWithSales(String name, Long salesCount) {
        Product p = createBaseProduct(name, new BigDecimal("100.00"));
        p.setSalesCount(salesCount);
        productRepository.save(p);
    }
}
