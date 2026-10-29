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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.users.dtos.EmailChangeRequestDTO;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.auth.PasswordResetDTO;
import ua.moki.modules.users.dtos.auth.VerifyEmailDTO;
import ua.moki.modules.users.dtos.auth.VerifyOtpDTO;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.services.AccountSecurityService;
import ua.moki.util.exceptions.InvalidTokenException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountSecurityController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
class AccountSecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountSecurityService accountSecurityService;

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
    @DisplayName("POST /users/activation - 200 OK when token is valid")
    void activateUser_shouldReturnOk_whenTokenIsValid() throws Exception {
        String token = "valid-activation-token";
        doNothing().when(accountSecurityService).activateUser(token);

        mockMvc.perform(post("/users/activation")
                        .param("token", token))
                .andExpect(status().isOk());

        verify(accountSecurityService).activateUser(token);
    }

    @Test
    @DisplayName("PATCH /users/profile/email - 200 OK on valid email change initiation")
    void changeEmail_shouldReturnOk_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "new.email@example.com",
                "CurrentStrongPass1!"
        );

        doNothing().when(accountSecurityService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/profile/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(accountSecurityService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/profile/email - 400 Bad Request when email format is invalid")
    void changeEmail_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO invalidDTO = new EmailChangeRequestDTO(
                "invalid-email-format",
                "CurrentPass1!"
        );

        mockMvc.perform(patch("/users/profile/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(accountSecurityService, never()).initiateEmailChange(any(), any());
    }

    @Test
    @DisplayName("PATCH /users/profile/email - 401 Unauthorized when current password is wrong")
    void changeEmail_shouldReturnUnauthorized_whenCurrentPasswordIsWrong() throws Exception {
        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "new@example.com",
                "WrongPass"
        );

        doThrow(new BadCredentialsException("Wrong password"))
                .when(accountSecurityService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/profile/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Wrong password")));
    }

    @Test
    @DisplayName("PATCH /users/profile/email - 400 Bad Request when new email is already taken")
    void changeEmail_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();
        EmailChangeRequestDTO requestDTO = new EmailChangeRequestDTO(
                "busy@example.com",
                "CorrectPass"
        );

        doThrow(new UserAlreadyExistsException("Email already taken"))
                .when(accountSecurityService).initiateEmailChange(eq(userId), any(EmailChangeRequestDTO.class));

        mockMvc.perform(patch("/users/profile/email")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Email already taken")));
    }

    @Test
    @DisplayName("POST /users/profile/email/confirm - 200 OK when token is valid")
    void confirmChange_shouldReturnOk_whenTokenIsValid() throws Exception {
        String token = "valid-email-token";
        doNothing().when(accountSecurityService).confirmEmailChange(token);

        mockMvc.perform(post("/users/profile/email/confirm")
                        .param("token", token))
                .andExpect(status().isOk());

        verify(accountSecurityService).confirmEmailChange(token);
    }

    @Test
    @DisplayName("POST /users/profile/email/confirm - 401 Unauthorized when token is expired")
    void confirmChange_shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {
        String token = "expired-token";
        doThrow(new InvalidTokenException("Token has expired"))
                .when(accountSecurityService).confirmEmailChange(token);

        mockMvc.perform(post("/users/profile/email/confirm")
                        .param("token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Token has expired")));
    }

    @Test
    @DisplayName("PATCH /users/profile/password - 204 No Content on valid password change")
    void changePassword_shouldReturnNoContent_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldStrongPass1!",
                "NewStrongPass1!",
                "NewStrongPass1!"
        );

        doNothing().when(accountSecurityService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));

        mockMvc.perform(patch("/users/profile/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNoContent());

        verify(accountSecurityService).changePassword(eq(userId), any(PasswordChangeRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /users/profile/password - 400 Bad Request when passwords do not match")
    void changePassword_shouldReturnBadRequest_whenPasswordsDoNotMatch() throws Exception {
        UUID userId = UUID.randomUUID();
        PasswordChangeRequestDTO requestDTO = new PasswordChangeRequestDTO(
                "OldStrongPass1!",
                "NewStrongPass1!",
                "MismatchPass2!"
        );

        mockMvc.perform(patch("/users/profile/password")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info.message", containsString("Passwords do not match")));

        verify(accountSecurityService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("POST /users/password-reset/initiate - 200 OK")
    void initiatePasswordReset_shouldReturnOk() throws Exception {
        VerifyEmailDTO dto = new VerifyEmailDTO("user@example.com");
        doNothing().when(accountSecurityService).initiateForgotPassword("user@example.com");

        mockMvc.perform(post("/users/password-reset/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(accountSecurityService).initiateForgotPassword("user@example.com");
    }

    @Test
    @DisplayName("POST /users/password-reset/verify - 200 OK with resetToken")
    void verifyOtp_shouldReturnResetToken() throws Exception {
        VerifyOtpDTO dto = new VerifyOtpDTO("user@example.com", "123456");
        when(accountSecurityService.verifyOtpAndGetResetToken("123456")).thenReturn("jwt-reset-token");

        mockMvc.perform(post("/users/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken", is("jwt-reset-token")));

        verify(accountSecurityService).verifyOtpAndGetResetToken("123456");
    }

    @Test
    @DisplayName("POST /users/password-reset/confirm - 200 OK when authenticated with OP_RESET_PASSWORD")
    void confirmPasswordReset_shouldReturnOk_whenAuthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        PasswordResetDTO dto = new PasswordResetDTO("NewStrongPass1!", "NewStrongPass1!");

        doNothing().when(accountSecurityService).resetPasswordWithJwt(eq(userId), any(PasswordResetDTO.class));

        mockMvc.perform(post("/users/password-reset/confirm")
                        .with(user(userId.toString()).authorities(new SimpleGrantedAuthority("OP_RESET_PASSWORD")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(accountSecurityService).resetPasswordWithJwt(eq(userId), any(PasswordResetDTO.class));
    }
}
