package ua.moki.modules.users.services.tokens;

import ua.moki.modules.users.domains.tokens.ActivationToken;
import ua.moki.modules.users.domains.User;

public interface ActivationTokenService extends TokenService<ActivationToken> {
    String generateToken(User user);
}
