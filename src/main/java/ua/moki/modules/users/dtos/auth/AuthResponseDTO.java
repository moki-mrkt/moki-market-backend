package ua.moki.modules.users.dtos.auth;


public record AuthResponseDTO(
                String accessToken,
                long expiresIn,
                String refreshToken,
                String tokenType
) {

}