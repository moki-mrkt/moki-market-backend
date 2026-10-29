package ua.moki.modules.orders.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.orders.domains.Cart;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.orders.repositories.CartRepository;
import ua.moki.modules.orders.services.impl.CartServiceImpl;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CartServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {

        testUser = new User();
        testUser.setPublicId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setSecondName("Test");
        testUser.setEmail(testUser.getPublicId() + "@test.com");
        testUser.setPhoneNumber("+3800000000");
        testUser.setPassword("pass");
        testUser.setRoleType(RoleType.CUSTOMER);
        testUser.setDeleted(false);
        userRepository.save(testUser);

        testProduct = new Product();
        testProduct.setName("Product");
        testProduct.setNameRu("Продукт");
        testProduct.setSlug("product-" + UUID.randomUUID());
        testProduct.setProductCategory(ProductCategory.DRIED_FRUITS);
        testProduct.setDescription("Test product");
        testProduct.setDescriptionRu("Тестовый продукт");
        testProduct.setPurchasePrice(BigDecimal.valueOf(100));
        testProduct.setDiscount(0);
        testProduct.setPrice(BigDecimal.valueOf(100));
        testProduct.setAvailability(ProductAvailability.IN_STOCK);
        testProduct.setInitOfMeasure("шт");
        testProduct.setValueOfInitOfMeasure(1);
        testProduct.setManufacturerOfTheProduct("Test Factory");
        testProduct.setSubcategory("General");
        testProduct.setSubcategoryRu("Общая");
        testProduct.setAllowCustomWeight(false);
        testProduct.setRating(BigDecimal.ZERO);
        testProduct.setCreationTime(OffsetDateTime.now());

        productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a new cart and add item if cart doesn't exist")
    void addToCart_shouldCreateCartAndAddItem() {

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 2, null);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().productId()).isEqualTo(testProduct.getId());
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);

        Cart savedCart = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(savedCart.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should update quantity if item already exists in cart")
    void addToCart_shouldUpdateExistingItemQuantity() {
        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 2, null);

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 3, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("Should add a different product to the existing cart")
    void addToCart_shouldAddDifferentProduct() {
        Product secondProduct = createAndSaveProduct(BigDecimal.valueOf(50.00));

        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 1, null);

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), secondProduct.getId(), 2, null);

        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("Should remove all items from the cart when clearCart is called")
    void clearCart_shouldRemoveAllItems() {
        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 5, null);

        Cart cartBefore = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cartBefore.getItems()).isNotEmpty();

        cartService.clearCart(testUser);

        Cart cartAfter = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cartAfter.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should do nothing and not throw exception when clearing non-existent cart")
    void clearCart_shouldDoNothing_whenCartDoesNotExist() {
        UUID userIdWithoutCart = UUID.randomUUID();

        cartService.clearCart(testUser);

        assertThat(cartRepository.findCartByUser_PublicId(userIdWithoutCart)).isEmpty();
    }

    @Test
    @DisplayName("Should only clear the cart of the specified user")
    void clearCart_shouldOnlyClearTargetUserCart() {

        User secondUser = createAndSaveUser();

        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 1, null);
        cartService.addToCart(secondUser.getPublicId(), testProduct.getId(), 3, null);

        cartService.clearCart(testUser);

        Cart cart1 = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cart1.getItems()).isEmpty();

        Cart cart2 = cartRepository.findCartByUser_PublicId(secondUser.getPublicId()).orElseThrow();
        assertThat(cart2.getItems()).hasSize(1);
        assertThat(cart2.getItems().getFirst().getQuantity()).isEqualTo(3);
    }

    private Product createAndSaveProduct(BigDecimal price) {
        Product product = new Product();
        product.setName("Test Product");
        product.setNameRu("Тестовый продукт");
        product.setSlug("test-product-" + UUID.randomUUID());
        product.setProductCategory(ProductCategory.DRIED_FRUITS);
        product.setDescription("Test product");
        product.setDescriptionRu("Тестовый продукт");
        product.setPurchasePrice(price);
        product.setDiscount(0);
        product.setPrice(BigDecimal.valueOf(100));
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setInitOfMeasure("шт");
        product.setValueOfInitOfMeasure(1);
        product.setManufacturerOfTheProduct("Test Factory");
        product.setSubcategory("General");
        product.setSubcategoryRu("Общая");
        product.setAllowCustomWeight(false);
        product.setRating(BigDecimal.ZERO);
        product.setCreationTime(OffsetDateTime.now());

        return productRepository.save(product);
    }

    private User createAndSaveUser() {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setSecondName("Test");
        user.setEmail("email@test.com");
        user.setPhoneNumber("+3800000000");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        return userRepository.save(user);
    }
}
