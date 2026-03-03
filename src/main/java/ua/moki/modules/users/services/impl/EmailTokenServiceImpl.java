package ua.moki.modules.users.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.EmailChangeToken;
import ua.moki.modules.users.repositories.EmailChangeTokenRepository;
import ua.moki.modules.users.services.tokens.AbstractTokenService;
import ua.moki.modules.users.services.tokens.EmailTokenService;

import java.util.UUID;

@Service
public class EmailTokenServiceImpl extends AbstractTokenService<EmailChangeToken> implements EmailTokenService {

    public EmailTokenServiceImpl(EmailChangeTokenRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public String generateToken(User user, String newEmail) {
        EmailChangeToken token = new EmailChangeToken();
        token.setNewEmail(newEmail);

        String rawToken = UUID.randomUUID().toString();

        return saveTokenEntity(user, token, rawToken, 15);
    }
}
