package ua.moki.modules.orders.services.jobs;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.moki.BaseIntegrationTest;
import ua.moki.modules.orders.domains.Cart;
import ua.moki.modules.orders.repositories.CartRepository;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CartCleanupJobTest extends BaseIntegrationTest {

    @Autowired
    private CartCleanupJob cartCleanupJob;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPublicId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setSecondName("Test");
        testUser.setEmail("email@test.com");
        testUser.setPhoneNumber("+3800000000");
        testUser.setPassword("pass");
        testUser.setRoleType(RoleType.CUSTOMER);
        testUser.setDeleted(false);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should delete carts updated more than 2 days ago")
    void deleteAbandonedCarts_shouldRemoveOldCarts() {

        Cart oldCart = new Cart();
        oldCart.setUser(testUser);

        cartRepository.saveAndFlush(oldCart);

        jdbcTemplate.update("UPDATE carts SET updated_at = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(3),
                oldCart.getId());

        entityManager.clear();

        assertThat(cartRepository.findAll()).hasSize(1);

        cartCleanupJob.deleteAbandonedCarts();

        assertThat(cartRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should keep carts updated less than 2 days ago")
    void deleteAbandonedCarts_shouldKeepRecentCarts() {

        Cart recentCart = new Cart();
        recentCart.setUser(testUser);
        recentCart.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        cartRepository.save(recentCart);

        cartCleanupJob.deleteAbandonedCarts();

        assertThat(cartRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should delete only old carts and keep recent ones")
    void deleteAbandonedCarts_shouldMixedScenario() {
        User secondUser = createAndSaveUser();

        Cart oldCart = new Cart();
        oldCart.setUser(testUser);
        cartRepository.saveAndFlush(oldCart);

        jdbcTemplate.update("UPDATE carts SET updated_at = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(5),
                oldCart.getId());

        Cart recentCart = new Cart();
        recentCart.setUser(secondUser);

        cartRepository.saveAndFlush(recentCart);

        entityManager.clear();

        cartCleanupJob.deleteAbandonedCarts();

        assertThat(cartRepository.findAll()).hasSize(1);
        assertThat(cartRepository.findCartByUser_PublicId(secondUser.getPublicId())).isPresent();
        assertThat(cartRepository.findCartByUser_PublicId(testUser.getPublicId())).isEmpty();
    }

    private User createAndSaveUser() {
        User testUser = new User();
        testUser.setPublicId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setSecondName("Test");
        testUser.setEmail("test@test.com");
        testUser.setPhoneNumber("+3800000000");
        testUser.setPassword("pass");
        testUser.setRoleType(RoleType.CUSTOMER);
        testUser.setDeleted(false);
        return userRepository.save(testUser);
    }
}
