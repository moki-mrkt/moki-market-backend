package ua.moki.modules.users.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.ActivationToken;
import ua.moki.modules.users.repositories.ActivationTokenRepository;
import ua.moki.modules.users.services.tokens.AbstractTokenService;
import ua.moki.modules.users.services.tokens.ActivationTokenService;

import java.util.UUID;

@Service
public class ActivationServiceImpl extends AbstractTokenService<ActivationToken> implements ActivationTokenService{

    public ActivationServiceImpl(ActivationTokenRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public String generateToken(User user) {
        ActivationToken token = new ActivationToken();
        String rawToken = UUID.randomUUID().toString();

        return saveTokenEntity(user, token, rawToken, 60);
    }
}
