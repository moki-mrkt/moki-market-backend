package ua.moki.modules.users.controllers;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.modules.users.dtos.*;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AppConfig.class, JwtCryptoConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("POST /register - successful registration (201 Created)")
    void register_shouldReturnCreated_whenRequestIsValid() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Ivan",
                "Ivanov",
                "ivan.test@mail.com",
                "+380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                "uuid-id",
                "Ivan",
                "Ivanov",
                "ivan.test@mail.com",
                "+380991234567",
                "",
                RoleType.CUSTOMER.name()
        );

          when(userService.createUser(any(UserCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/users/uuid-id"))
                .andExpect(jsonPath("$.id", is("uuid-id")))
                .andExpect(jsonPath("$.email", is("ivan.test@mail.com")));

        verify(userService).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /register - 400 Bad Request if the passwords do not match (@PasswordMatches)")
    void register_shouldReturnBadRequest_whenPasswordsDoNotMatch() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Ivan",
                "Ivanov",
                "valid@mail.com",
                "+380991234567",
                "StrongPass1!",
                "DifferentPass2!"
        );

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info.message", containsString("Passwords do not match")));
    }

    @Test
    @DisplayName("POST /register - 400 Bad Request if the name is lowercase (Regex)")
    void register_shouldReturnBadRequest_whenNameFormatIsInvalid() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "ivan",
                "Ivanov",
                "valid@mail.com",
                "+380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register - 400 Bad Request if the password is too simple (Regex)")
    void register_shouldReturnBadRequest_whenPasswordIsTooSimple() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Ivan",
                "Ivanov",
                "valid@mail.com",
                "+380991234567",
                "simplepass",
                "simplepass"
        );

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register - 400 Bad Request if the phone is without a plus")
    void register_shouldReturnBadRequest_whenPhoneIsInvalid() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Ivan",
                "Ivanov",
                "valid@mail.com",
                "380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/managers - successful manager creation (201 Created)")
    void createManager_shouldReturnCreated_whenRequestIsValid() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Manager",
                "User",
                "manager.test@mail.com",
                "+380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                "manager-uuid",
                "Manager",
                "User",
                "manager.test@mail.com",
                "+380991234567",
                "",
                RoleType.MANAGER.name()
        );

        when(userService.createManager(any(UserCreateDTO.class))).thenReturn(responseDTO);


        mockMvc.perform(post("/users/managers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("manager-uuid")))
                .andExpect(jsonPath("$.roleType", is("MANAGER")))
                .andExpect(jsonPath("$.email", is("manager.test@mail.com")));

        verify(userService).createManager(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /users/managers - returns 400 Bad Request if the data is invalid")
    void createManager_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {

        UserCreateDTO invalidDTO = new UserCreateDTO(
                "",
                "User",
                "invalid-email",
                "+380991234567",
                "pass",
                "pass"
        );

        mockMvc.perform(post("/users/managers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createManager(any());
    }

    @Test
    @DisplayName("POST /users/managers - return 403 Forbidden, if user isn't admin")
    void createManager_returnNot_whenUserNotAdmin() throws Exception {

        UserCreateDTO invalidDTO = new UserCreateDTO(
                "",
                "User",
                "invalid-email",
                "+380991234567",
                "pass",
                "pass"
        );

        mockMvc.perform(post("/users/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createManager(any());
    }

    @Test
    @DisplayName("PATCH /users/profile - successfully updates the authenticated user's profile (200 OK)")
    void updateProfile_shouldReturnUpdatedUser_whenRequestIsValid() throws Exception {

        UUID userId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1990, 5, 20);

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                "NewFirstName",
                "NewSecondName",
                "+380991234567",
                birthDate
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                userId.toString(),
                "NewFirstName",
                "NewSecondName",
                "test@mail.com",
                "+380991234567",
                "",
                "CUSTOMER"
        );

        when(userService.updateUser(eq(userId), any(UserUpdateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/users/profile")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.firstName", is("NewFirstName")))
                .andExpect(jsonPath("$.phoneNumber", is("+380991234567")));

        verify(userService).updateUser(eq(userId), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/profile - returns 400 Bad Request if the data is invalid")
    void updateProfile_shouldReturnBadRequest_whenDataIsInvalid() throws Exception {

        UUID userId = UUID.randomUUID();

        UserUpdateDTO invalidDTO = new UserUpdateDTO(
                "lowercase",
                "ValidSurname",
                "380991234567",
                LocalDate.now()
        );

        mockMvc.perform(patch("/users/profile")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/profile - returns 401/403 if the user is not authenticated")
    void updateProfile_shouldReturnUnauthorized_whenUserNotAuthenticated() throws Exception {

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                "Name",
                "Surname",
                "+380991234567",
                LocalDate.now()
        );

        mockMvc.perform(patch("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/{id} - successfully updates user data by admin (200 OK)")
    void updateUser_shouldReturnUpdatedUser_whenUserExists() throws Exception {

        UUID userId = UUID.randomUUID();

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                "UpdatedName",
                "UpdatedSurname",
                "+380998887766",
                null
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                userId.toString(),
                "UpdatedName",
                "UpdatedSurname",
                "test@mail.com",
                "+380998887766",
                "",
                "CUSTOMER"
        );

        when(userService.updateUser(eq(userId), any(UserUpdateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/users/{id}", userId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.firstName", is("UpdatedName")))
                .andExpect(jsonPath("$.phoneNumber", is("+380998887766")));

        verify(userService).updateUser(eq(userId), any(UserUpdateDTO.class));
    }


    @Test
    @DisplayName("PATCH /user/{id} - 400 Bad Request if the name is lowercase (Regex)")
    void updateUser_throwException_whenDataIsInvalid() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "ivan",
                "Ivanov",
                "valid@mail.com",
                "+380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        mockMvc.perform(patch("/users/1")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("PATCH /user/{id} - 403 Forbidden if the user is not admin")
    void updateUser_throwException_whenUserNotAdmin() throws Exception {

        UserCreateDTO requestDTO = new UserCreateDTO(
                "Ivan",
                "Ivanov",
                "valid@mail.com",
                "+380991234567",
                "StrongPass1!",
                "StrongPass1!"
        );

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/email - successful initiation of email change (200 OK)")
    void changeEmail_shouldReturnOk_whenRequestIsValid() throws Exception {

        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "new.email@example.com",
                "CurrentStrongPass1!"
        );

        doNothing().when(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/email - returns 400 Bad Request if the email format is incorrect")
    void changeEmail_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {

        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO invalidDTO = new EmailChangeRequestDTO(
                "invalid-email-format",
                "CurrentPass1!"
        );

        mockMvc.perform(patch("/users/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info.newEmail").exists());

        verify(userService, never()).initiateEmailChange(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/email - returns 400 Bad Request if password is empty")
    void changeEmail_shouldReturnBadRequest_whenPasswordIsEmpty() throws Exception {

        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO invalidDTO = new EmailChangeRequestDTO(
                "valid@example.com",
                ""
        );

        mockMvc.perform(patch("/users/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).initiateEmailChange(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/email - returns 401 Unauthorized if the current password is incorrect")
    void changeEmail_shouldReturnUnauthorized_whenCurrentPasswordIsWrong() throws Exception {

        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "new@example.com",
                "WrongPass"
        );

        doThrow(new org.springframework.security.authentication.BadCredentialsException("Wrong password"))
                .when(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Wrong password")));

        verify(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/email - returns 400 Bad Request if the new email is already taken")
    void changeEmail_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {

        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "busy@example.com",
                "CorrectPass"
        );

        doThrow(new ua.moki.util.exceptions.UserAlreadyExistsException("Email already taken"))
                .when(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Email already taken")));

        verify(userService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/email - returns 403 Forbidden if the user is not logged in")
    void changeEmail_shouldReturnForbidden_whenNotAuthenticated() throws Exception {

        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "new@example.com",
                "Pass"
        );

        mockMvc.perform(patch("/users/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /users/email/confirm - successful confirmation of email change (200 OK)")
    void confirmChange_shouldReturnOk_whenTokenIsValid() throws Exception {

        String token = "valid.jwt.token";

        doNothing().when(userService).confirmEmailChange(token);

        mockMvc.perform(post("/users/email/confirm")
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).confirmEmailChange(token);
    }

    @Test
    @DisplayName("POST /users/email/confirm - returns 401 Unauthorized if the token is invalid or expired")
    void confirmChange_shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {

        String token = "invalid.token";

        doThrow(new ua.moki.util.exceptions.InvalidTokenException("Token expired or invalid"))
                .when(userService).confirmEmailChange(token);

        mockMvc.perform(post("/users/email/confirm")
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Token expired or invalid")));

        verify(userService).confirmEmailChange(token);
    }
    @Test
    @DisplayName("POST /users/email/confirm - returns 400 Bad Request if the email is already busy")
    void confirmChange_shouldReturnBadRequest_whenEmailTaken() throws Exception {

        String token = "valid.token.but.email.taken";

        doThrow(new ua.moki.util.exceptions.UserAlreadyExistsException("Email already taken"))
                .when(userService).confirmEmailChange(token);

        mockMvc.perform(post("/users/email/confirm")
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Email already taken")));

        verify(userService).confirmEmailChange(token);
    }
    @Test
    @DisplayName("POST /users/email/confirm - returns 400 Bad Request if the token parameter is missing")
    void confirmChange_shouldReturnBadRequest_whenParamIsMissing() throws Exception {

        mockMvc.perform(post("/users/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, never()).confirmEmailChange(any());
    }

    @Test
    @DisplayName("PATCH /users/password - successful password change (204 No Content)")
    void changePassword_shouldReturnNoContent_whenRequestIsValid() throws Exception {

        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldStrongPass1!",
                "NewStrongPass1!",
                "NewStrongPass1!"
        );

        doNothing().when(userService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));

        mockMvc.perform(patch("/users/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/password - returns 400 Bad Request if passwords do not match")
    void changePassword_shouldReturnBadRequest_whenPasswordsDoNotMatch() throws Exception {

        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldStrongPass1!",
                "NewStrongPass1!",
                "MismatchPass2!"
        );

        mockMvc.perform(patch("/users/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info.message", containsString("Passwords do not match")));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/password - returns 400 Bad Request if the new password is too simple")
    void changePassword_shouldReturnBadRequest_whenNewPasswordIsSimple() throws Exception {

        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldStrongPass1!",
                "simple",
                "simple"
        );

        mockMvc.perform(patch("/users/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/password - returns 401 Unauthorized if the current password is incorrect")
    void changePassword_shouldReturnUnauthorized_whenCurrentPasswordIsWrong() throws Exception {

        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "WrongOldPass1!",
                "NewStrongPass1!",
                "NewStrongPass1!"
        );

        doThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid current password"))
                .when(userService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));

        mockMvc.perform(patch("/users/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Invalid current password")));

        verify(userService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/password - returns 403 Forbidden if the user is not authenticated")
    void changePassword_shouldReturnForbidden_whenNotAuthenticated() throws Exception {

        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldPass1!", "NewPass1!", "NewPass1!"
        );

        mockMvc.perform(patch("/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - successful change of blocking status by admin (204 No Content)")
    void switchBlockStatus_shouldReturnNoContent_whenAdmin() throws Exception {

        UUID userId = UUID.randomUUID();
        boolean isBlocked = true;

        doNothing().when(userService).updateBlockStatus(userId, isBlocked);

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .with(user("admin").roles("ADMIN"))
                        .param("isBlocked", String.valueOf(isBlocked))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).updateBlockStatus(userId, isBlocked);
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - successful change of blocking status by manager (204 No Content)")
    void switchBlockStatus_shouldReturnNoContent_whenManager() throws Exception {

        UUID userId = UUID.randomUUID();
        boolean isBlocked = false;

        doNothing().when(userService).updateBlockStatus(userId, isBlocked);

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .with(user("manager").roles("MANAGER"))
                        .param("isBlocked", String.valueOf(isBlocked))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).updateBlockStatus(userId, isBlocked);
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - returns 403 Forbidden for a regular user")
    void switchBlockStatus_shouldReturnForbidden_whenCustomer() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .with(user("customer").roles("CUSTOMER"))
                        .param("isBlocked", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateBlockStatus(any(), anyBoolean());
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - returns 403 Forbidden if not authorized")
    void switchBlockStatus_shouldReturnForbidden_whenNotAuthenticated() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .param("isBlocked", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateBlockStatus(any(), anyBoolean());
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - return 404 Not Found if user not exists")
    void switchBlockStatus_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {

        UUID userId = UUID.randomUUID();
        boolean isBlocked = true;

        doThrow(new EntityNotFoundException("User not found"))
                .when(userService).updateBlockStatus(userId, isBlocked);

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .with(user("admin").roles("ADMIN"))
                        .param("isBlocked", String.valueOf(isBlocked))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found")));

        verify(userService).updateBlockStatus(userId, isBlocked);
    }

    @Test
    @DisplayName("PATCH /users/{id}/block-status - returns 400 Bad Request if the isBlocked parameter is missing")
    void switchBlockStatus_shouldReturnBadRequest_whenParamMissing() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/block-status", userId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateBlockStatus(any(), anyBoolean());
    }

    @Test
    @DisplayName("DELETE /users/{id} - returns 204 No Content on successful deletion")
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {

        UUID userId = UUID.randomUUID();

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /users/{id} - returns 404 Not Found if the user does not exist")
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {

        UUID nonExistentId = UUID.randomUUID();

        doThrow(new EntityNotFoundException("User not found"))
                .when(userService).deleteUser(nonExistentId);

        mockMvc.perform(delete("/users/{id}", nonExistentId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found")));

        verify(userService).deleteUser(nonExistentId);
    }

    @Test
    @DisplayName("DELETE /profile - successful deletion of your own account (204 No Content)")
    void deleteCurrentAccount_shouldReturnNoContent_whenUserIsAuthenticated() throws Exception {

        String userUuid = "123e4567-e89b-12d3-a456-426614174000";
        UUID uuid = UUID.fromString(userUuid);

        doNothing().when(userService).deleteUser(uuid);

        mockMvc.perform(delete("/users/profile")
                        .with(user(userUuid).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(uuid);
    }

    @Test
    @DisplayName("DELETE /profile - 403 Forbidden if user is not logged in")
    void deleteCurrentAccount_shouldReturnUnauthorized_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(delete("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    @DisplayName("GET /users/{id} - returns 200 OK and DTO if user found")
    void getUserById_shouldReturnUser_whenUserExists() throws Exception {

        UUID userId = UUID.randomUUID();
        UserResponseDTO responseDTO = new UserResponseDTO(
                userId.toString(),
                "Ivan",
                "Ivanov",
                "test@mail.com",
                "+380991234567",
                "",
                "CUSTOMER"

        );

        when(userService.getActiveUserByPublicId(userId)).thenReturn(responseDTO);

        mockMvc.perform(get("/users/{id}", userId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.firstName", is("Ivan")))
                .andExpect(jsonPath("$.email", is("test@mail.com")));

        verify(userService).getActiveUserByPublicId(userId);
    }

    @Test
    @DisplayName("GET /users/{id} - returns 403 Forbidden if the user does not admin")
    void getUserById_shouldReturnForbidden_whenUserNotAdmin() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getActiveUserByPublicId(userId);
    }

    @Test
    @DisplayName("GET /users/{id} - returns 404 Not Found if the user does not exist or has been deleted")
    void getUserById_shouldReturnNotFound_whenUserNotFound() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.getActiveUserByPublicId(userId))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}", userId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found")));

        verify(userService).getActiveUserByPublicId(userId);
    }

    @Test
    @DisplayName("GET /users/profile - returns the current user's profile (200 OK)")
    void getUserById_shouldReturnCurrentUser_whenAuthenticated() throws Exception {

        String userUuidString = "123e4567-e89b-12d3-a456-426614174000";
        UUID userUuid = UUID.fromString(userUuidString);

        UserResponseDTO responseDTO = new UserResponseDTO(
                userUuidString,
                "Ivan",
                "Ivanov",
                "ivan.test@mail.com",
                "+380991234567",
                "image-id",
                "CUSTOMER"
        );

        when(userService.getActiveUserByPublicId(userUuid)).thenReturn(responseDTO);

        mockMvc.perform(get("/users/profile")
                        .with(user(userUuidString).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userUuid.toString())))
                .andExpect(jsonPath("$.email", is("ivan.test@mail.com")))
                .andExpect(jsonPath("$.firstName", is("Ivan")));

        verify(userService).getActiveUserByPublicId(userUuid);
    }

    @Test
    @DisplayName("GET /users/profile - 403 Forbidden for anonymous user")
    void getUserById_shouldReturnUnauthorized_whenUserIsAnonymous() throws Exception {

        mockMvc.perform(get("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getActiveUserByPublicId(any());
    }

    @Test
    @DisplayName("GET /users/all - returns all users (no filter) when the parameters are default")
    void getAllUsers_shouldReturnAllUsers_whenNoFilterProvided() throws Exception {

        int page = 0;
        int size = 10;

        UserResponseDTO userDTO = new UserResponseDTO(
                UUID.randomUUID().toString(), "Test", "User", "test@mail.com", "+380991234567", "","CUSTOMER"
        );
        PageImpl<UserResponseDTO> pageResult = new PageImpl<>(List.of(userDTO));

        when(userService.getAllUser(isNull(), eq(PageRequest.of(page, size))))
                .thenReturn(pageResult);

        mockMvc.perform(get("/users/all")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("test@mail.com")));

        verify(userService).getAllUser(isNull(), eq(PageRequest.of(page, size)));
    }

    @Test
    @DisplayName("GET /users/all - returns only deleted users (?deleted=true)")
    void getAllUsers_shouldReturnDeletedUsers_whenFilterIsTrue() throws Exception {

        boolean deleted = true;

        UserResponseDTO deletedUserDTO = new UserResponseDTO(
                UUID.randomUUID().toString(), "Deleted User", "", "del@mail.com_deleted", "+38000_deleted", "","CUSTOMER"
        );
        PageImpl<UserResponseDTO> pageResult = new PageImpl<>(List.of(deletedUserDTO));

        when(userService.getAllUser(eq(true), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/users/all")
                        .with(user("admin").roles("ADMIN"))
                        .param("deleted", String.valueOf(deleted))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName", is("Deleted User")));

        verify(userService).getAllUser(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /users/all - returns only active users (?deleted=false)")
    void getAllUsers_shouldReturnActiveUsers_whenFilterIsFalse() throws Exception {

        int page = 1;
        int size = 5;

        when(userService.getAllUser(eq(false), eq(PageRequest.of(page, size))))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/users/all")
                        .with(user("admin").roles("ADMIN"))
                        .param("deleted", "false")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(userService).getAllUser(eq(false), eq(PageRequest.of(page, size)));
    }

    @Test
    @DisplayName("GET /users/all - 403 Forbidden for anonymous user")
    void getAllUsers_shouldReturnForbidden_whenUserIsAnonymous() throws Exception {

        int page = 1;
        int size = 5;

        mockMvc.perform(get("/users/all")
                        .param("deleted", "false")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUser(eq(false), eq(PageRequest.of(page, size)));
    }

}