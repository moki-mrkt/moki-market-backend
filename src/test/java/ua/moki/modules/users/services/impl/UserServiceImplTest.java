package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.BeforeEach;
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
import ua.moki.modules.users.domains.RefreshToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.UserCreateDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.dtos.UserUpdateDTO;
import ua.moki.modules.users.repositories.RefreshTokenRepository;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.EmailTokenService;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailTokenService emailTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

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
    @DisplayName("createManager successfully creates a user with the MANAGER role and active access rights")
    void createManager_shouldCreateActiveManager_whenRequestIsValid() {

        String rawPassword = "StrongManagerPass1!";
        UserCreateDTO managerDTO = new UserCreateDTO(
                "Manager",
                "Test",
                "new.manager@company.com",
                "+380930000001",
                rawPassword,
                rawPassword
        );

        UserResponseDTO result = userService.createManager(managerDTO);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("new.manager@company.com");
        assertThat(result.roleType()).isEqualTo(RoleType.MANAGER.name());

        User savedManager = userRepository.findByPublicIdAndDeletedFalse(UUID.fromString(result.id()))
                .orElseThrow(() -> new AssertionError("Manager not found in DB"));

        assertThat(savedManager.getRoleType()).isEqualTo(RoleType.MANAGER);


        assertThat(savedManager.isActivated()).isTrue();
        assertThat(savedManager.isAccessToAccount()).isTrue();
        assertThat(savedManager.isBlocked()).isFalse();

        assertThat(savedManager.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, savedManager.getPassword())).isTrue();
    }

    @Test
    @DisplayName("createManager throws UserAlreadyExistsException if email is already taken")
    void createManager_shouldThrowException_whenEmailIsAlreadyTaken() {

        User existingUser = new User();
        existingUser.setFirstName("Existing");
        existingUser.setSecondName("User");
        existingUser.setEmail("busy.email@test.com");
        existingUser.setPhoneNumber("+380990000000");
        existingUser.setPassword("pass");
        existingUser.setRoleType(RoleType.CUSTOMER);
        userRepository.save(existingUser);

        UserCreateDTO duplicateEmailDTO = new UserCreateDTO(
                "New",
                "Manager",
                "busy.email@test.com",
                "+380931112233",
                "Pass123!",
                "Pass123!"
        );

        assertThatThrownBy(() -> userService.createManager(duplicateEmailDTO))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already taken");
    }

    @Test
    @DisplayName("updateUser successfully updates user data")
    void updateUser_shouldUpdateFields_whenUserExists() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("OldName");
        user.setSecondName("OldSurname");
        user.setEmail("update.test@mail.com");
        user.setPhoneNumber("+380991111111");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        userRepository.save(user);

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                "NewName",
                "NewSurname",
                "+380992222222",
                LocalDate.of(2000, 1, 1)
        );

        UserResponseDTO result = userService.updateUser(user.getPublicId(), updateDTO);

        assertThat(result.firstName()).isEqualTo("NewName");
        assertThat(result.secondName()).isEqualTo("NewSurname");
        assertThat(result.phoneNumber()).isEqualTo("+380992222222");

        User updatedUser = userRepository.findByPublicId(user.getPublicId()).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("NewName");
        assertThat(updatedUser.getSecondName()).isEqualTo("NewSurname");
        assertThat(updatedUser.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("updateUser ignores null fields in DTO (does not overwrite existing data)")
    void updateUser_shouldIgnoreNullFields_whenDtoHasNulls() {
        // Given
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("OriginalName");
        user.setSecondName("OriginalSurname");
        user.setEmail("partial.update@mail.com");
        user.setPhoneNumber("+380991111111");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        userRepository.save(user);

        UserUpdateDTO partialUpdateDTO = new UserUpdateDTO(
                "UpdatedOnlyName",
                null,
                null,
                null
        );

        userService.updateUser(user.getPublicId(), partialUpdateDTO);

        User updatedUser = userRepository.findByPublicId(user.getPublicId()).orElseThrow();

        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedOnlyName");
        assertThat(updatedUser.getSecondName()).isEqualTo("OriginalSurname");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("+380991111111");
    }

    @Test
    @DisplayName("updateUser throws EntityNotFoundException if the user is deleted (soft delete)")
    void updateUser_shouldThrowException_whenUserIsDeleted() {

        User deletedUser = new User();
        deletedUser.setPublicId(UUID.randomUUID());
        deletedUser.setFirstName("Deleted");
        deletedUser.setSecondName("User");
        deletedUser.setEmail("deleted.update@mail.com");
        deletedUser.setPassword("pass");
        deletedUser.setRoleType(RoleType.CUSTOMER);
        deletedUser.setDeleted(true); // Юзер видалений
        userRepository.save(deletedUser);

        UserUpdateDTO updateDTO = new UserUpdateDTO("New", "Name", "+380000000000", null);

        assertThatThrownBy(() -> userService.updateUser(deletedUser.getPublicId(), updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("updateUser throws EntityNotFoundException if ID does not exist")
    void updateUser_shouldThrowException_whenIdDoesNotExist() {

        UUID nonExistentId = UUID.randomUUID();
        UserUpdateDTO updateDTO = new UserUpdateDTO("New", "Name", "+380000000000", null);

        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateDTO))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("updateBlockStatus blocks the user and removes all their refresh tokens")
    void updateBlockStatus_shouldBlockUserAndRevokeTokens() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Block");
        user.setSecondName("Candidate");
        user.setEmail("block.me@test.com");
        user.setPhoneNumber("+380991112233");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setBlocked(false);
        user.setDeleted(false);
        userRepository.save(user);

        ua.moki.modules.users.domains.RefreshToken token = new ua.moki.modules.users.domains.RefreshToken();
        token.setToken("token_to_be_revoked");
        token.setUser(user);
        token.setExpiryDate(java.time.Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

        assertThat(refreshTokenRepository.findByToken("token_to_be_revoked")).isPresent();

        userService.updateBlockStatus(user.getPublicId(), true); // Блокуємо

        User blockedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(blockedUser.isBlocked()).isTrue();

        assertThat(refreshTokenRepository.findByToken("token_to_be_revoked")).isEmpty();
    }

    @Test
    @DisplayName("updateBlockStatus unblocks the user (without removing tokens)")
    void updateBlockStatus_shouldUnblockUser() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Unblock");
        user.setSecondName("Me");
        user.setEmail("unblock.me@test.com");
        user.setPhoneNumber("+380993334455");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setBlocked(true);
        user.setDeleted(false);
        userRepository.save(user);

        userService.updateBlockStatus(user.getPublicId(), false);

        User unblockedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unblockedUser.isBlocked()).isFalse();
    }

    @Test
    @DisplayName("updateBlockStatus does nothing if the status is already the same")
    void updateBlockStatus_shouldDoNothing_whenStatusIsSame() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Same");
        user.setSecondName("Status");
        user.setEmail("same.status@test.com");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setBlocked(true);
        userRepository.save(user);

        userService.updateBlockStatus(user.getPublicId(), true);

        User sameUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(sameUser.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("updateBlockStatus throws EntityNotFoundException if user is not found or deleted")
    void updateBlockStatus_shouldThrowException_whenUserNotFound() {
        assertThatThrownBy(() -> userService.updateBlockStatus( UUID.randomUUID(), true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("confirmEmailChange successfully changes the email and invalidates the refresh tokens")
    void confirmEmailChange_shouldUpdateEmailAndRevokeTokens_whenTokenIsValid() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Change");
        user.setSecondName("Email");
        user.setEmail("old.email@test.com");
        user.setPhoneNumber("+380991112233");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        userRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setToken("token_to_revoke_after_email_change");
        token.setUser(user);
        token.setExpiryDate(java.time.Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

        String newEmail = "new.super.email@test.com";
        String confirmationToken = emailTokenService.generateEmailChangeToken(user.getPublicId(), newEmail);

        userService.confirmEmailChange(confirmationToken);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);

        assertThat(refreshTokenRepository.findByToken("token_to_revoke_after_email_change")).isEmpty();
    }

    @Test
    @DisplayName("confirmEmailChange throws UserAlreadyExistsException if the new email is already taken")
    void confirmEmailChange_shouldThrowException_whenNewEmailIsTaken() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("User");
        user.setSecondName("One");
        user.setEmail("user1@test.com");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        userRepository.save(user);

        User occupier = new User();
        occupier.setPublicId(UUID.randomUUID());
        occupier.setFirstName("Occupier");
        occupier.setSecondName("User");
        occupier.setEmail("taken@test.com");
        occupier.setPassword("pass");
        occupier.setRoleType(RoleType.CUSTOMER);
        userRepository.save(occupier);

        String confirmationToken = emailTokenService.generateEmailChangeToken(user.getPublicId(), "taken@test.com");

        assertThatThrownBy(() -> userService.confirmEmailChange(confirmationToken))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already taken");
    }

    @Test
    @DisplayName("confirmEmailChange throws InvalidTokenException if the token is 'broken'")
    void confirmEmailChange_shouldThrowException_whenTokenIsInvalid() {

        String invalidToken = "invalid.jwt.token";

        assertThatThrownBy(() -> userService.confirmEmailChange(invalidToken))
                .isInstanceOf(ua.moki.util.exceptions.InvalidTokenException.class);
    }

    @Test
    @DisplayName("changePassword successfully changes the password and deletes all active sessions")
    void changePassword_shouldUpdatePasswordAndRevokeTokens_whenRequestIsValid() {

        String oldPassword = "OldStrongPassword1!";
        String newPassword = "NewStrongPassword2@";

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Change");
        user.setSecondName("Pass");
        user.setEmail("changepass@test.com");
        user.setPassword(passwordEncoder.encode(oldPassword));
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        userRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setToken("token_before_pass_change");
        token.setUser(user);
        token.setExpiryDate(java.time.Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

          PasswordChangeRequestDTO changeDTO = new PasswordChangeRequestDTO(
                oldPassword,
                newPassword,
                newPassword
        );

        userService.changePassword(user.getPublicId(), changeDTO);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(oldPassword, updatedUser.getPassword())).isFalse();

        assertThat(refreshTokenRepository.findByToken("token_before_pass_change")).isEmpty();
    }

    @Test
    @DisplayName("changePassword throws BadCredentialsException if the old password is incorrect")
    void changePassword_shouldThrowException_whenCurrentPasswordIsIncorrect() {

        String realPassword = "RealPassword123";
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Wrong");
        user.setSecondName("Pass");
        user.setEmail("wrong.pass@test.com");
        user.setPassword(passwordEncoder.encode(realPassword));
        user.setRoleType(RoleType.CUSTOMER);
        userRepository.save(user);

        PasswordChangeRequestDTO invalidDTO = new PasswordChangeRequestDTO(
                "NewPass123!",
                "NewPass123!",
                "WrongOldPass"
        );

        assertThatThrownBy(() -> userService.changePassword(user.getPublicId(), invalidDTO))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage("Invalid current password");
    }

    @Test
    @DisplayName("changePassword throws IllegalArgumentException if the new password matches the old one")
    void changePassword_shouldThrowException_whenNewPasswordIsSameAsOld() {

        String samePassword = "SamePassword1!";
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Same");
        user.setSecondName("Pass");
        user.setEmail("same.pass@test.com");
        user.setPassword(passwordEncoder.encode(samePassword));
        user.setRoleType(RoleType.CUSTOMER);
        userRepository.save(user);

        PasswordChangeRequestDTO samePassDTO = new PasswordChangeRequestDTO(
                samePassword, samePassword, samePassword
        );

        assertThatThrownBy(() -> userService.changePassword(user.getPublicId(), samePassDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password cannot be the same as old password");
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
    @DisplayName("getUserById returns the user entity when the ID exists in the database")
    void getUserById_shouldReturnUser_whenIdExists() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Internal");
        user.setSecondName("IdSearch");
        user.setEmail("internal.id@test.com");
        user.setPhoneNumber("+380998887766");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);

        User savedUser = userRepository.save(user);
        Long generatedId = savedUser.getId();

        User result = userService.getUserById(generatedId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(generatedId);
        assertThat(result.getEmail()).isEqualTo("internal.id@test.com");
    }

    @Test
    @DisplayName("getUserById throws EntityNotFoundException if a user with that ID does not exist")
    void getUserById_shouldThrowException_whenIdDoesNotExist() {

        Long nonExistentId = Long.MAX_VALUE;

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("getActiveUserEntityByPublicId returns the entity if the user exists and has not been deleted")
    void getActiveUserEntityByPublicId_shouldReturnUser_whenUserIsActive() {

        User activeUser = new User();
        activeUser.setPublicId(UUID.randomUUID());
        activeUser.setFirstName("Active");
        activeUser.setSecondName("User");
        activeUser.setEmail("active.get@test.com");
        activeUser.setPhoneNumber("+380991234567");
        activeUser.setPassword("pass");
        activeUser.setRoleType(RoleType.CUSTOMER);
        activeUser.setDeleted(false); // Активний
        userRepository.save(activeUser);

        User result = userService.getActiveUserEntityByPublicId(activeUser.getPublicId());

        assertThat(result).isNotNull();
        assertThat(result.getPublicId()).isEqualTo(activeUser.getPublicId());
        assertThat(result.getEmail()).isEqualTo("active.get@test.com");
    }

    @Test
    @DisplayName("getActiveUserEntityByPublicId throws EntityNotFoundException if user is marked as deleted")
    void getActiveUserEntityByPublicId_shouldThrowException_whenUserIsDeleted() {

        User deletedUser = new User();
        deletedUser.setPublicId(UUID.randomUUID());
        deletedUser.setFirstName("Deleted");
        deletedUser.setSecondName("User");
        deletedUser.setEmail("deleted.get@test.com");
        deletedUser.setPassword("pass");
        deletedUser.setRoleType(RoleType.CUSTOMER);
        deletedUser.setDeleted(true);
        userRepository.save(deletedUser);

        assertThatThrownBy(() -> userService.getActiveUserEntityByPublicId(deletedUser.getPublicId()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getActiveUserEntityByPublicId throws EntityNotFoundException if ID does not exist")
    void getActiveUserEntityByPublicId_shouldThrowException_whenIdDoesNotExist() {

        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.getActiveUserEntityByPublicId(randomId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getActiveUserByPublicId returns a DTO if the user is active")
    void getActiveUserByPublicId_shouldReturnDto_whenUserIsActive() {

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setFirstName("Dto");
        user.setSecondName("Check");
        user.setEmail("dto.check@test.com");
        user.setPhoneNumber("+380995556677");
        user.setPassword("pass");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);
        user.setImageId("test-image-id");
        userRepository.save(user);

        UserResponseDTO result = userService.getActiveUserByPublicId(user.getPublicId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(user.getPublicId().toString());
        assertThat(result.firstName()).isEqualTo("Dto");
        assertThat(result.secondName()).isEqualTo("Check");
        assertThat(result.email()).isEqualTo("dto.check@test.com");
        assertThat(result.imageUrl()).isEqualTo("http://localhost:9000/moki-images/test-image-id");
        assertThat(result.roleType()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("getActiveUserByPublicId throws EntityNotFoundException if user is deleted")
    void getActiveUserByPublicId_shouldThrowException_whenUserIsDeleted() {

        User deletedUser = new User();
        deletedUser.setPublicId(UUID.randomUUID());
        deletedUser.setFirstName("Deleted");
        deletedUser.setSecondName("One");
        deletedUser.setEmail("deleted.dto@test.com");
        deletedUser.setPassword("pass");
        deletedUser.setRoleType(RoleType.CUSTOMER);
        deletedUser.setDeleted(true);
        userRepository.save(deletedUser);

        assertThatThrownBy(() -> userService.getActiveUserByPublicId(deletedUser.getPublicId()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getActiveUserByPublicId throws EntityNotFoundException if ID is invalid")
    void getActiveUserByPublicId_shouldThrowException_whenIdDoesNotExist() {

        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.getActiveUserByPublicId(randomId))
                .isInstanceOf(EntityNotFoundException.class);
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
