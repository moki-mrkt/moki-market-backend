package ua.moki.modules.users.services;

import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.security.Token;

public interface RefreshTokenService {

    void saveRefreshToken(String newRefreshTokenString, Token newRefreshTokenObj, User user);
    void deleteRefreshToken(String token);
    void deleteAllForUser(Long userId);
    User consumeRefreshTokenAndGetUser(String oldRefreshTokenString);
}
