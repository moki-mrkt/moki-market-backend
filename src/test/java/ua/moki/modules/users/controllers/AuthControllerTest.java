package ua.moki.modules.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.modules.users.dtos.auth.AuthResponseDTO;
import ua.moki.modules.users.dtos.auth.LoginRequestDTO;
import ua.moki.modules.users.dtos.auth.LogoutRequestDTO;
import ua.moki.modules.users.dtos.auth.RefreshTokenRequestDTO;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.services.impl.AuthServiceImpl;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AppConfig.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthServiceImpl authService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/login - повертає 200 OK і токени при успішному вході")
    void login_shouldReturnOk_whenCredentialsAreValid() throws Exception {

        LoginRequestDTO loginRequest = new LoginRequestDTO("test@mail.com", "password123");

        AuthResponseDTO authResponse = new AuthResponseDTO(
                "access_token_xyz",
                3600L,
                "refresh_token_xyz",
                "Bearer"
        );

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("access_token_xyz")))
                .andExpect(jsonPath("$.refreshToken", is("refresh_token_xyz")));

        verify(authService).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - повертає 400 Bad Request, якщо дані невалідні (@Valid)")
    void login_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {

        LoginRequestDTO invalidRequest = new LoginRequestDTO("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("POST /auth/refresh - повертає 200 OK і нові токени")
    void refresh_shouldReturnOk_whenRefreshTokenIsValid() throws Exception {

        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO("valid_refresh_token");

        AuthResponseDTO authResponse = new AuthResponseDTO(
                "new_access_token",
                3600L,
                "refresh_token_xyz",
                "Bearer"
        );

        when(authService.refreshAccessToken(any(RefreshTokenRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("new_access_token")));

        verify(authService).refreshAccessToken(any(RefreshTokenRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/logout - повертає 204 No Content")
    void logout_shouldReturnNoContent() throws Exception {

        LogoutRequestDTO logoutRequest = new LogoutRequestDTO("refresh_token_to_revoke");

        doNothing().when(authService).logout(logoutRequest.refreshToken());

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        verify(authService).logout(logoutRequest.refreshToken());
    }
}
