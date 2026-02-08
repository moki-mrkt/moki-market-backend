package ua.moki.modules.feedbacks.services;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.modules.feedback.domains.Feedback;
import ua.moki.modules.feedback.domains.ProductFeedback;
import ua.moki.modules.feedback.domains.StoreFeedback;
import ua.moki.modules.feedback.dtos.FeedbackAnswerDTO;
import ua.moki.modules.feedback.dtos.FeedbackRequestDTO;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;
import ua.moki.modules.feedback.dtos.FeedbackUpdateDTO;
import ua.moki.modules.feedback.repositories.FeedbackRepository;
import ua.moki.modules.feedback.services.FeedbackService;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class FeedbackServiceImplTest {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private FeedbackRepository feedbackRepository;

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
        testProduct.setProductCategory(ProductCategory.DRIED_FRUITS);
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
        feedbackRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully create ProductFeedback when productId is provided")
    void createFeedback_shouldCreateProductFeedback() {
        FeedbackRequestDTO dto = new FeedbackRequestDTO(testProduct.getId(), "Great phone!", 5);

        FeedbackResponseDTO response = feedbackService.createFeedback(testUser.getPublicId(), dto);

        assertThat(response).isNotNull();
        assertThat(response.comment()).isEqualTo("Great phone!");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.firstNameUser()).isEqualTo("Test");

        Feedback saved = feedbackRepository.findById(response.id()).orElseThrow();
        assertThat(saved).isInstanceOf(ProductFeedback.class);
        assertThat(((ProductFeedback) saved).getProduct().getId()).isEqualTo(testProduct.getId());
    }

    @Test
    @DisplayName("Should successfully create StoreFeedback when productId is null")
    void createFeedback_shouldCreateStoreFeedback() {
        FeedbackRequestDTO dto = new FeedbackRequestDTO(null, "Great phone!", 4);

        FeedbackResponseDTO response = feedbackService.createFeedback(testUser.getPublicId(), dto);

        assertThat(response.id()).isNotNull();
        Feedback saved = feedbackRepository.findById(response.id()).orElseThrow();
        assertThat(saved).isInstanceOf(StoreFeedback.class);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void createFeedback_shouldThrowException_whenUserNotFound() {

        FeedbackRequestDTO dto = new FeedbackRequestDTO(null, "Great phone!", 5);
        UUID randomId = UUID.randomUUID();

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.createFeedback(randomId, dto)
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product does not exist")
    void createFeedback_shouldThrowException_whenProductNotFound() {

        FeedbackRequestDTO dto = new FeedbackRequestDTO(9999L, "Great phone!", 5);

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.createFeedback(testUser.getPublicId(), dto)
        );
    }

    @Test
    @DisplayName("Should successfully update comment and rating of an existing feedback")
    void updateFeedback_shouldUpdateData() {

        FeedbackRequestDTO createDto = new FeedbackRequestDTO(testProduct.getId(), "Old comment", 3);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), createDto);

        FeedbackUpdateDTO updateDto = new FeedbackUpdateDTO("Updated comment", 5);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getPublicId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                ));

        FeedbackResponseDTO updated = feedbackService.updateFeedback(created.id(), updateDto, SecurityContextHolder.getContext().getAuthentication());

        assertThat(updated.comment()).isEqualTo("Updated comment");
        assertThat(updated.rating()).isEqualTo(5);

        Feedback saved = feedbackRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getComment()).isEqualTo("Updated comment");
        assertThat(saved.getRating()).isEqualTo(5);

        assertThat(saved).isInstanceOf(ProductFeedback.class);
        assertThat(((ProductFeedback) saved).getProduct().getId()).isEqualTo(testProduct.getId());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent feedback")
    void updateFeedback_shouldThrowException_whenNotFound() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getPublicId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                ));

        FeedbackUpdateDTO updateDto = new FeedbackUpdateDTO("Comment", 5);
        Long nonExistentId = 999L;

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.updateFeedback(nonExistentId, updateDto, SecurityContextHolder.getContext().getAuthentication())
        );
    }

    @Test
    @DisplayName("Should not affect answer fields when updating basic feedback info")
    void updateFeedback_shouldNotChangeAnswerFields() {

        ProductFeedback feedback = new ProductFeedback();
        feedback.setUser(testUser);
        feedback.setProduct(testProduct);
        feedback.setComment("Comment");
        feedback.setRating(3);
        feedback.setAnswer("Admin answer");
        feedback.setAnsweredAt(OffsetDateTime.now());
        Feedback savedInitial = feedbackRepository.save(feedback);

        FeedbackUpdateDTO updateDto = new FeedbackUpdateDTO("New user comment", 4);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getPublicId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                ));

        FeedbackResponseDTO response = feedbackService.updateFeedback(savedInitial.getId(), updateDto, SecurityContextHolder.getContext().getAuthentication());

        assertThat(response.comment()).isEqualTo("New user comment");
        assertThat(response.answer()).isEqualTo("Admin answer");
        assertThat(response.answeredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully add admin answer to an existing feedback")
    void addAnswerToFeedback_shouldAddAnswerAndTimestamp() {

        FeedbackRequestDTO createDto = new FeedbackRequestDTO(testProduct.getId(), "Where is my order?", 3);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), createDto);

        FeedbackAnswerDTO answerDto = new FeedbackAnswerDTO("We are checking your order status.");

        FeedbackResponseDTO responded = feedbackService.addAnswerToFeedback(created.id(), "ROLE_ADMIN", answerDto);

        assertThat(responded.answer()).isEqualTo("We are checking your order status.");
        assertThat(responded.answeredAt()).isNotNull();
        assertThat(responded.answeredAt()).isAfter(OffsetDateTime.now().minusSeconds(5));

        Feedback saved = feedbackRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getAnswer()).isEqualTo("We are checking your order status.");
        assertThat(saved.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when adding answer to non-existent feedback")
    void addAnswerToFeedback_shouldThrowException_whenNotFound() {
        FeedbackAnswerDTO answerDto = new FeedbackAnswerDTO("Answer");
        Long nonExistentId = 9999L;

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.addAnswerToFeedback(nonExistentId, "ROLE_ADMIN", answerDto)
        );
    }

    @Test
    @DisplayName("Should successfully add answer to ProductFeedback and preserve product link")
    void addAnswerToFeedback_shouldPreserveProductLink() {

        FeedbackRequestDTO createDto = new FeedbackRequestDTO(testProduct.getId(), "Help", 3);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), createDto);

        FeedbackAnswerDTO answerDto = new FeedbackAnswerDTO("Thank you for your feedback!");

        feedbackService.addAnswerToFeedback(created.id(), "ROLE_ADMIN", answerDto);

        Feedback saved = feedbackRepository.findById(created.id()).orElseThrow();

        assertThat(saved).isInstanceOf(ProductFeedback.class);
        assertThat(((ProductFeedback) saved).getProduct().getId()).isEqualTo(testProduct.getId());
        assertThat(saved.getAnswer()).isEqualTo("Thank you for your feedback!");
    }

    @Test
    @DisplayName("Should allow overwriting an existing answer with a new one")
    void addAnswerToFeedback_shouldOverwriteExistingAnswer() {
        FeedbackRequestDTO createDto = new FeedbackRequestDTO(testProduct.getId(), "Help", 3);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), createDto);

        feedbackService.addAnswerToFeedback(created.id(),"ROLE_ADMIN", new FeedbackAnswerDTO("First answer"));
        OffsetDateTime firstAnswerTime = feedbackRepository.findById(created.id()).get().getAnsweredAt();

        FeedbackAnswerDTO newAnswerDto = new FeedbackAnswerDTO("Revised answer");
        FeedbackResponseDTO updated = feedbackService.addAnswerToFeedback(created.id(), "ROLE_ADMIN", newAnswerDto);

        assertThat(updated.answer()).isEqualTo("Revised answer");
        assertThat(updated.answeredAt()).isAfterOrEqualTo(firstAnswerTime);
    }

    @Test
    @DisplayName("Should successfully delete feedback by owner")
    void deleteFeedback_shouldDeleteWhenOwner() {

        FeedbackRequestDTO dto = new FeedbackRequestDTO(null,"To be deleted", 5);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), dto);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getPublicId().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));


        feedbackService.deleteFeedback(created.id(), SecurityContextHolder.getContext().getAuthentication());

        assertThat(feedbackRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("Should successfully delete feedback by admin even if not owner")
    void deleteFeedback_shouldDeleteWhenAdmin() {

        FeedbackRequestDTO dto = new FeedbackRequestDTO(null,"Admin will delete this", 1);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), dto);
        UUID adminId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));

        feedbackService.deleteFeedback(created.id(), SecurityContextHolder.getContext().getAuthentication());

        assertThat(feedbackRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner tries to delete")
    void deleteFeedback_shouldThrowException_whenNotOwner() {

        FeedbackRequestDTO dto = new FeedbackRequestDTO(null, "Private feedback", 5);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), dto);
        UUID strangerId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(strangerId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                ));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                feedbackService.deleteFeedback(created.id(), SecurityContextHolder.getContext().getAuthentication())
        );

        assertThat(feedbackRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when feedback doesn't exist")
    void deleteFeedback_shouldThrowException_whenNotFound() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getPublicId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.deleteFeedback(999L, SecurityContextHolder.getContext().getAuthentication())
        );
    }

    @Test
    @DisplayName("Should return average rating for a specific product")
    void getAverageRatingForProduct_shouldReturnCorrectAverage() {

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(testProduct.getId(), "Good", 5));
        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO( testProduct.getId(), "Average", 4));
        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(testProduct.getId(), "Bad", 2));

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(null, "Store feedback", 1));

        BigDecimal average = feedbackService.getAverageRatingForProduct(testProduct.getId());

        assertThat(average).isNotNull();
        assertThat(average).isEqualByComparingTo("3.7");
    }

    @Test
    @DisplayName("Should successfully return FeedbackResponseDTO when feedback exists")
    void getFeedbackById_shouldReturnFeedbackResponseDTO() {

        FeedbackRequestDTO requestDto = new FeedbackRequestDTO(testProduct.getId(), "Excellent product!", 5);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), requestDto);

        FeedbackResponseDTO result = feedbackService.getFeedbackById(created.id());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(created.id());
        assertThat(result.comment()).isEqualTo("Excellent product!");
        assertThat(result.rating()).isEqualTo(5);

        assertThat(result.firstNameUser()).isEqualTo("Test");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 0.0 or default when product has no ratings")
    void getAverageRatingForProduct_shouldReturnZero_whenNoFeedbacks() {

        BigDecimal average = feedbackService.getAverageRatingForProduct(testProduct.getId());

        assertThat(average).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("Should return average rating for the store")
    void getAverageRatingForStore_shouldReturnCorrectAverage() {

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(null, "Great shop", 5));
        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(null, "OK", 3));

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(testProduct.getId(), "Product", 1));

        BigDecimal average = feedbackService.getAverageRatingForStore();

        assertThat(average).isEqualByComparingTo("4.0");
    }

    @Test
    @DisplayName("Should return average rating for store even if multiple users leave feedback")
    void getAverageRatingForStore_shouldAggregateAllUsers() {

        User user2 = new User();
        user2.setPublicId(UUID.randomUUID());
        user2.setFirstName("Test");
        user2.setSecondName("Test");
        user2.setEmail("another@test.com");
        user2.setPhoneNumber("+3800000000");
        user2.setPassword("password");
        user2.setRoleType(RoleType.CUSTOMER);
        user2.setDeleted(false);
        userRepository.save(user2);

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(null, "User1 says 5", 5));
        feedbackService.createFeedback(user2.getPublicId(), new FeedbackRequestDTO(null, "User2 says 1", 1));

        java.math.BigDecimal average = feedbackService.getAverageRatingForStore();

        assertThat(average).isEqualByComparingTo("3.0");
    }

    @Test
    @DisplayName("Should return feedback with admin answer if it exists")
    void getFeedbackById_shouldReturnFeedbackWithAnswer() {
        FeedbackRequestDTO requestDto = new FeedbackRequestDTO(null, "Wait for it", 3);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), requestDto);

        FeedbackAnswerDTO answerDto = new FeedbackAnswerDTO("Answer from admin");
        feedbackService.addAnswerToFeedback(created.id(), "ROLE_ADMIN", answerDto);

        FeedbackResponseDTO result = feedbackService.getFeedbackById(created.id());

        assertThat(result.answer()).isEqualTo("Answer from admin");
        assertThat(result.answeredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when feedback does not exist")
    void getFeedbackById_shouldThrowException_whenNotFound() {

        Long nonExistentId = 9999L;

        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.getFeedbackById(nonExistentId)
        );
    }

    @Test
    @DisplayName("Should return paged feedbacks for a specific user sorted by creation date")
    void getFeedbacksByUserId_shouldReturnPagedFeedbacks() {

        FeedbackRequestDTO feedback1 = new FeedbackRequestDTO(null, "First feedback", 3);
        FeedbackRequestDTO feedback2 = new FeedbackRequestDTO( testProduct.getId(), "Second feedback", 5);

        feedbackService.createFeedback(testUser.getPublicId(), feedback1);
        feedbackService.createFeedback(testUser.getPublicId(), feedback2);

        User anotherUser = new User();
        anotherUser.setPublicId(UUID.randomUUID());
        anotherUser.setFirstName("Test");
        anotherUser.setSecondName("Test");
        anotherUser.setEmail("another@test.com");
        anotherUser.setPhoneNumber("+3800000000");
        anotherUser.setPassword("password");
        anotherUser.setRoleType(RoleType.CUSTOMER);
        anotherUser.setDeleted(false);
        userRepository.save(anotherUser);
        feedbackService.createFeedback(anotherUser.getPublicId(), new FeedbackRequestDTO(null, "Other user feedback", 1));

        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksByUserId(testUser.getPublicId(), 0, 10);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).comment()).isEqualTo("Second feedback");
        assertThat(result.getContent().get(1).comment()).isEqualTo("First feedback");

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return paged feedbacks for a specific product sorted by creation date")
    void getFeedbacksByProductId_shouldReturnPagedFeedbacks() {

        FeedbackRequestDTO feedback1 = new FeedbackRequestDTO(testProduct.getId(), "Good quality", 4);
        feedbackService.createFeedback(testUser.getPublicId(), feedback1);

        FeedbackRequestDTO feedback2 = new FeedbackRequestDTO( testProduct.getId(), "Amazing product!", 5);
        feedbackService.createFeedback(testUser.getPublicId(), feedback2);

        feedbackService.createFeedback(testUser.getPublicId(), new FeedbackRequestDTO(null, "Nice shop", 5));


        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksByProductId(testProduct.getId(), 0, 10);

        // Then: перевіряємо кількість та сортування (descending)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).comment()).isEqualTo("Amazing product!");
        assertThat(result.getContent().get(1).comment()).isEqualTo("Good quality");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty page when product has no feedbacks")
    void getFeedbacksByProductId_shouldReturnEmptyPage_whenNoFeedbacks() {

        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksByProductId(testProduct.getId(), 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should correctly map all fields including admin answer in paged results")
    void getFeedbacksByProductId_shouldMapAllFieldsCorrectly() {

        FeedbackRequestDTO request = new FeedbackRequestDTO(testProduct.getId(), "Need help", 2);
        FeedbackResponseDTO created = feedbackService.createFeedback(testUser.getPublicId(), request);

        feedbackService.addAnswerToFeedback(created.id(), "ROLE_ADMIN", new FeedbackAnswerDTO("How can we help?"));

        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksByProductId(testProduct.getId(), 0, 5);

        FeedbackResponseDTO dto = result.getContent().getFirst();
        assertThat(dto.comment()).isEqualTo("Need help");
        assertThat(dto.answer()).isEqualTo("How can we help?");
        assertThat(dto.answeredAt()).isNotNull();
        assertThat(dto.firstNameUser()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Should handle pagination correctly for product feedbacks")
    void getFeedbacksByProductId_shouldHandlePagination() {
        for (int i = 1; i <= 3; i++) {
            feedbackService.createFeedback(testUser.getPublicId(),
                    new FeedbackRequestDTO(testProduct.getId(), "Feedback " + i, 5));
        }

        Page<FeedbackResponseDTO> page = feedbackService.getFeedbacksByProductId(testProduct.getId(), 0, 2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return only store feedbacks sorted by creation date")
    void getFeedbacksAboutStore_shouldReturnOnlyStoreFeedbacks() {

        FeedbackRequestDTO storeFeedback1 = new FeedbackRequestDTO(null, "First store feedback", 5);
        feedbackService.createFeedback(testUser.getPublicId(), storeFeedback1);

        FeedbackRequestDTO storeFeedback2 = new FeedbackRequestDTO(null, "Second store feedback", 4);
        feedbackService.createFeedback(testUser.getPublicId(), storeFeedback2);

        FeedbackRequestDTO productFeedback = new FeedbackRequestDTO(testProduct.getId(), "Product feedback", 5);
        feedbackService.createFeedback(testUser.getPublicId(), productFeedback);

        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksAboutStore(0, 10);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).comment()).isEqualTo("Second store feedback");
        assertThat(result.getContent().get(1).comment()).isEqualTo("First store feedback");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty page when there are no store feedbacks")
    void getFeedbacksAboutStore_shouldReturnEmptyPage_whenNoData() {

        FeedbackRequestDTO productFeedback = new FeedbackRequestDTO(testProduct.getId(), "Only product", 5);
        feedbackService.createFeedback(testUser.getPublicId(), productFeedback);

        Page<FeedbackResponseDTO> result = feedbackService.getFeedbacksAboutStore(0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should correctly handle pagination for store feedbacks")
    void getFeedbacksAboutStore_shouldHandlePagination() {

        for (int i = 1; i <= 3; i++) {
            feedbackService.createFeedback(testUser.getPublicId(),
                    new FeedbackRequestDTO(null,"Store feedback " + i, 5));
        }

        Page<FeedbackResponseDTO> page = feedbackService.getFeedbacksAboutStore(0, 2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
