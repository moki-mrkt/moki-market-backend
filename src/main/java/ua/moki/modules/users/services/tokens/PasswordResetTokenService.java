package ua.moki.modules.users.services.tokens;

import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.PasswordResetToken;

public interface PasswordResetTokenService extends TokenService<PasswordResetToken> {
    String generateToken(User user);
}
