package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.UserCreateDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("createUser successfully saves the user to the DB with a hashed password and the CUSTOMER role")
    void createUser_shouldPersistUser_whenDataIsValid() {

        String rawPassword = "securePassword123";
        UserCreateDTO createDTO = new UserCreateDTO(
                "John",
                "Doe",
                "john.doe@example.com",
                "+380991234567",
                rawPassword,
                rawPassword
        );

        UserResponseDTO result = userService.createUser(createDTO);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        assertThat(result.id()).isNotNull();

        User savedUser = userRepository.findByPublicIdAndDeletedFalse(UUID.fromString(result.id())).orElseThrow();

        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getSecondName()).isEqualTo("Doe");
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("+380991234567");

        assertThat(savedUser.getRoleType()).isEqualTo(RoleType.CUSTOMER);

        assertThat(savedUser.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("createUser throws UserAlreadyExistsException if email is already taken")
    void createUser_shouldThrowException_whenEmailAlreadyExists() {

        User existingUser = new User();
        existingUser.setEmail("duplicate@example.com");
        existingUser.setFirstName("First");
        existingUser.setSecondName("User");
        existingUser.setPassword("pass");
        existingUser.setRoleType(RoleType.CUSTOMER);
        userRepository.save(existingUser);

        UserCreateDTO duplicateDTO = new UserCreateDTO(
                "Second",
                "User",
                "duplicate@example.com",
                "+380000000000",
                "newpass",
                "newpass"
        );

        assertThatThrownBy(() -> userService.createUser(duplicateDTO))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already taken");
    }

    @Test
    @DisplayName("deleteUser performs a Soft Delete and anonymizes user data")
    void deleteUser_shouldSoftDeleteAndAnonymizeUser() {

        UUID publicId = UUID.randomUUID();
        String originalEmail = "delete.me@example.com";
        String originalPhone = "+380991112233";

        User user = new User();

        user.setPublicId(publicId);
        user.setFirstName("ToBeDeleted");
        user.setSecondName("User");
        user.setEmail(originalEmail);
        user.setPhoneNumber(originalPhone);
        user.setPassword("secretPass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        user.setActivated(true);
        user.setBlocked(false);
        user.setSubscribedToNews(true);
        user.setAccessToAccount(true);

        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setImageId("some-image-id");

        userRepository.save(user);

        userService.deleteUser(publicId);

        User deletedUser = userRepository.findByPublicId(publicId).orElseThrow();

        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(deletedUser.isBlocked()).isTrue();
        assertThat(deletedUser.isActivated()).isFalse();
        assertThat(deletedUser.isAccessToAccount()).isFalse();
        assertThat(deletedUser.isSubscribedToNews()).isFalse();

        assertThat(deletedUser.getEmail()).startsWith(originalEmail + "_deleted_");
        assertThat(deletedUser.getPhoneNumber()).startsWith(originalPhone + "_deleted_");

        assertThat(deletedUser.getFirstName()).isEqualTo("Deleted User");
        assertThat(deletedUser.getSecondName()).isEmpty();
        assertThat(deletedUser.getPassword()).isEmpty();
        assertThat(deletedUser.getImageId()).isNull();
        assertThat(deletedUser.getDateOfBirth()).isNull();
    }

    @Test
    @DisplayName("deleteUser throws EntityNotFoundException if the user does not exist")
    void deleteUser_shouldThrowException_whenUserNotFound() {

        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getAllUser returns ALL users (active and deleted) when isDeleted == null")
    void getAllUser_shouldReturnAllUsers_whenIsDeletedIsNull() {

        User activeUser = new User();
        activeUser.setPublicId(UUID.randomUUID());
        activeUser.setFirstName("Active");
        activeUser.setSecondName("User");
        activeUser.setEmail("active@test.com");
        activeUser.setPhoneNumber("+380991111111");
        activeUser.setPassword("pass");
        activeUser.setRoleType(RoleType.CUSTOMER);
        activeUser.setDeleted(false);
        userRepository.save(activeUser);

        User deletedUser = new User();
        deletedUser.setPublicId(UUID.randomUUID());
        deletedUser.setFirstName("Deleted User"); // Анонімізоване ім'я
        deletedUser.setSecondName("");
        deletedUser.setEmail("deleted@test.com_deleted_123");
        deletedUser.setPhoneNumber("+380992222222_deleted_123");
        deletedUser.setPassword("");
        deletedUser.setRoleType(RoleType.CUSTOMER);
        deletedUser.setDeleted(true);
        userRepository.save(deletedUser);

        Page<UserResponseDTO> result = userService.getAllUser(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2); // Має знайти обох

        List<String> emails = result.getContent().stream()
                .map(UserResponseDTO::email)
                .toList();
        assertThat(emails).contains("active@test.com", "deleted@test.com_deleted_123");
    }

    @Test
    @DisplayName("getAllUser returns ONLY ACTIVE users when isDeleted == false")
    void getAllUser_shouldReturnOnlyActiveUsers_whenIsDeletedIsFalse() {

        createTestUser("Active One", false);
        createTestUser("Deleted One", true);
        createTestUser("Active Two", false);

        Page<UserResponseDTO> result = userService.getAllUser(false, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(UserResponseDTO::firstName)
                .containsExactlyInAnyOrder("Active One", "Active Two");

        assertThat(result.getContent())
                .extracting(UserResponseDTO::firstName)
                .doesNotContain("Deleted One");
    }

    @Test
    @DisplayName("getAllUser returns ONLY DELETED users when isDeleted == true")
    void getAllUser_shouldReturnOnlyDeletedUsers_whenIsDeletedIsTrue() {

        createTestUser("Active User", false);
        createTestUser("Deleted User 1", true);
        createTestUser("Deleted User 2", true);

        Page<UserResponseDTO> result = userService.getAllUser(true, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(UserResponseDTO::firstName)
                .containsExactlyInAnyOrder("Deleted User 1", "Deleted User 2");
    }

    @Test
    @DisplayName("getAllUser correctly handles pagination")
    void getAllUser_shouldRespectPagination() {

        for (int i = 1; i <= 5; i++) {
            createTestUser("User " + i, false);
        }

        Page<UserResponseDTO> page1 = userService.getAllUser(false, PageRequest.of(0, 2));

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    private void createTestUser(String firstName, boolean isDeleted) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName(firstName);
        user.setSecondName("Test");
        user.setEmail(firstName.replace(" ", "") + UUID.randomUUID() + "@test.com");
        user.setPhoneNumber("+380" + UUID.randomUUID().toString().hashCode());
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(isDeleted);
        userRepository.save(user);
    }
}
