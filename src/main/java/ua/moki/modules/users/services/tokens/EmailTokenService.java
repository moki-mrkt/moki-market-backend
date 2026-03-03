package ua.moki.modules.users.services.tokens;

import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.EmailChangeToken;

public interface EmailTokenService extends TokenService<EmailChangeToken> {

    String generateToken(User user, String newEmail);
}
