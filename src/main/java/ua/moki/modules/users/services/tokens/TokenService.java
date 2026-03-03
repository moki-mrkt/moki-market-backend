package ua.moki.modules.users.services.tokens;

import ua.moki.modules.users.domains.tokens.BaseToken;

import java.util.Optional;

public interface TokenService<T extends BaseToken> {
    T findByToken(String token);
    void deleteToken(T token);
    void deleteAllByUserId(Long userId);
    Optional<T> findFirstByUserId(Long userId);
}