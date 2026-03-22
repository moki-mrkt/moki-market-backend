package ua.moki.modules.users.services.tokens;

import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.ActivationToken;
import ua.moki.modules.users.domains.tokens.BaseToken;
import ua.moki.modules.users.repositories.BaseTokenRepository;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.OffsetDateTime;
import java.util.Optional;

public class AbstractTokenService<T extends BaseToken> implements TokenService<T> {

    protected final BaseTokenRepository<T> repository;

    protected AbstractTokenService(BaseTokenRepository<T> repository) {
        this.repository = repository;
    }

    protected String saveTokenEntity(User user, T tokenEntity, String rawToken, int validMinutes) {

        repository.deleteAllByUserId(user.getId());

        tokenEntity.setUser(user);
        tokenEntity.setToken(rawToken);
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusMinutes(validMinutes));

        repository.save(tokenEntity);

        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public T findByToken(String token) {
        return repository.findByToken(token).orElseThrow(
                () -> new InvalidTokenException("Invalid token")
        );
    }

    @Override
    @Transactional
    public void deleteToken(T token) {
        repository.delete(token);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(Long userId) {
        repository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findFirstByUserId(Long userId) {
        return repository.findFirstByUserId(userId);
    }
}
