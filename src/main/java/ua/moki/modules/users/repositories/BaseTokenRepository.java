package ua.moki.modules.users.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import ua.moki.modules.users.domains.tokens.BaseToken;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseTokenRepository<T extends BaseToken> extends JpaRepository<T, UUID> {
    Optional<T> findByToken(String token);
    void deleteAllByUserId(Long userId);
    Optional<T> findFirstByUserId(Long userId);
}
