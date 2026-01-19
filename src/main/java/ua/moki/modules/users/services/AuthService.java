package ua.moki.modules.users.services;

import ua.moki.modules.users.dtos.auth.AuthResponseDTO;
import ua.moki.modules.users.dtos.auth.LoginRequestDTO;
import ua.moki.modules.users.dtos.auth.RefreshTokenRequestDTO;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refreshAccessToken(RefreshTokenRequestDTO request);
    void logout(String refreshTokenString);
}
