package ua.moki.modules.orders.services;

import org.aspectj.lang.annotation.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
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

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
public class CartServiceImplTest {

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
        testProduct.setProductCategory(ProductCategory.DRY_FRUITS);
        testProduct.setDescription("Test product");
        testProduct.setPurchasePrice(BigDecimal.valueOf(100));
        testProduct.setDiscount(0);
        testProduct.setPrice(BigDecimal.valueOf(100));
        testProduct.setAvailability(ProductAvailability.IN_STOCK);
        testProduct.setInitOfMeasure("шт");
        testProduct.setValueOfInitOfMeasure(1);
        testProduct.setManufacturerOfTheProduct("Test Factory");
        testProduct.setSubcategory("General");
        testProduct.setRating(BigDecimal.ZERO);
        testProduct.setCreationTime(OffsetDateTime.now());

        productRepository.save(testProduct);
    }

    @AfterEach
    void  tearDown() {
        cartRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a new cart and add item if cart doesn't exist")
    void addToCart_shouldCreateCartAndAddItem() {

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 2);

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
        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 2);

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 3);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("Should add a different product to the existing cart")
    void addToCart_shouldAddDifferentProduct() {
        Product secondProduct = createAndSaveProduct(BigDecimal.valueOf(50.00));

        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 1);

        CartResponseDTO response = cartService.addToCart(testUser.getPublicId(), secondProduct.getId(), 2);

        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("Should remove all items from the cart when clearCart is called")
    void clearCart_shouldRemoveAllItems() {
        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 5);

        Cart cartBefore = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cartBefore.getItems()).isNotEmpty();

        cartService.clearCart(testUser.getPublicId());

        Cart cartAfter = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cartAfter.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should do nothing and not throw exception when clearing non-existent cart")
    void clearCart_shouldDoNothing_whenCartDoesNotExist() {
        UUID userIdWithoutCart = UUID.randomUUID();

        cartService.clearCart(userIdWithoutCart);

        assertThat(cartRepository.findCartByUser_PublicId(userIdWithoutCart)).isEmpty();
    }

    @Test
    @DisplayName("Should only clear the cart of the specified user")
    void clearCart_shouldOnlyClearTargetUserCart() {

        User secondUser = createAndSaveUser();

        cartService.addToCart(testUser.getPublicId(), testProduct.getId(), 1);
        cartService.addToCart(secondUser.getPublicId(), testProduct.getId(), 3);

        cartService.clearCart(testUser.getPublicId());

        Cart cart1 = cartRepository.findCartByUser_PublicId(testUser.getPublicId()).orElseThrow();
        assertThat(cart1.getItems()).isEmpty();

        Cart cart2 = cartRepository.findCartByUser_PublicId(secondUser.getPublicId()).orElseThrow();
        assertThat(cart2.getItems()).hasSize(1);
        assertThat(cart2.getItems().getFirst().getQuantity()).isEqualTo(3);
    }

    private Product createAndSaveProduct(BigDecimal price) {
        Product testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setProductCategory(ProductCategory.DRY_FRUITS);
        testProduct.setDescription("Test product");
        testProduct.setPurchasePrice(price);
        testProduct.setDiscount(0);
        testProduct.setPrice(BigDecimal.valueOf(100));
        testProduct.setAvailability(ProductAvailability.IN_STOCK);
        testProduct.setInitOfMeasure("шт");
        testProduct.setValueOfInitOfMeasure(1);
        testProduct.setManufacturerOfTheProduct("Test Factory");
        testProduct.setSubcategory("General");
        testProduct.setRating(BigDecimal.ZERO);
        testProduct.setCreationTime(OffsetDateTime.now());

        return productRepository.save(testProduct);
    }

    private User createAndSaveUser() {
        User testUser = new User();
        testUser.setPublicId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setSecondName("Test");
        testUser.setEmail("email@test.com");
        testUser.setPhoneNumber("+3800000000");
        testUser.setPassword("pass");
        testUser.setRoleType(RoleType.CUSTOMER);
        testUser.setDeleted(false);
        return userRepository.save(testUser);
    }
}
