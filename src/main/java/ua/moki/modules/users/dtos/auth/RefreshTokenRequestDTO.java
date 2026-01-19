package ua.moki.modules.users.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
