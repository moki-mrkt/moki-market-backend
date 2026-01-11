package ua.moki.modules.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.JacksonConfig;
import ua.moki.modules.users.dtos.UserCreateDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.dtos.UserUpdateDTO;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(JacksonConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("PATCH /users/{id} - successfully updates user data (200 OK)")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.firstName", is("UpdatedName")))
                .andExpect(jsonPath("$.phoneNumber", is("+380998887766")));

        verify(userService).updateUser(eq(userId), any(UserUpdateDTO.class));
    }

    @Test
    @DisplayName("DELETE /users/{id} - returns 204 No Content on successful deletion")
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {

        UUID userId = UUID.randomUUID();

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /users/{id} - returns 404 Not Found if the user does not exist")
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {

        UUID nonExistentId = UUID.randomUUID();

        doThrow(new EntityNotFoundException("User not found"))
                .when(userService).deleteUser(nonExistentId);

        mockMvc.perform(delete("/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found")));

        verify(userService).deleteUser(nonExistentId);
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
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.firstName", is("Ivan")))
                .andExpect(jsonPath("$.email", is("test@mail.com")));

        verify(userService).getActiveUserByPublicId(userId);
    }

    @Test
    @DisplayName("GET /users/{id} - returns 404 Not Found if the user does not exist or has been deleted")
    void getUserById_shouldReturnNotFound_whenUserNotFound() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.getActiveUserByPublicId(userId))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("User not found")));

        verify(userService).getActiveUserByPublicId(userId);
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
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("test@mail.com")));

        verify(userService).getAllUser(isNull(), eq(PageRequest.of(page, size)));
    }

    @Test
    @DisplayName("GET /users/all - returns only deleted users (?deleted=true)")
    void getAllUsers_shouldReturnDeletedUsers_whenFilterIsTrue() throws Exception {

        int page = 0;
        int size = 10;
        boolean deleted = true;

        UserResponseDTO deletedUserDTO = new UserResponseDTO(
                UUID.randomUUID().toString(), "Deleted User", "", "del@mail.com_deleted", "+38000_deleted", "","CUSTOMER"
        );
        PageImpl<UserResponseDTO> pageResult = new PageImpl<>(List.of(deletedUserDTO));

        when(userService.getAllUser(eq(true), any(Pageable.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/users/all")
                        .param("deleted", "true")
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
                        .param("deleted", "false")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(userService).getAllUser(eq(false), eq(PageRequest.of(page, size)));
    }

}