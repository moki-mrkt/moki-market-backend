package ua.moki.modules.users.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponseDTO(
        @JsonProperty("access_token")
                String accessToken,
        @JsonProperty("expires_in")
                long expiresIn,
        @JsonProperty("refresh_token")
                String refreshToken,
        @JsonProperty("token_type")
                String tokenType
) {

}