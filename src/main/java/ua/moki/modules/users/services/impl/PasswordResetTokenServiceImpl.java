package ua.moki.modules.users.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.tokens.PasswordResetToken;
import ua.moki.modules.users.repositories.PasswordResetTokenRepository;
import ua.moki.modules.users.services.tokens.AbstractTokenService;
import ua.moki.modules.users.services.tokens.PasswordResetTokenService;
import ua.moki.util.OtpGenerator;

@Service
public class PasswordResetTokenServiceImpl
        extends AbstractTokenService<PasswordResetToken>
        implements PasswordResetTokenService {

    private final OtpGenerator otpGenerator;

    public PasswordResetTokenServiceImpl(PasswordResetTokenRepository repository, OtpGenerator otpGenerator) {
        super(repository);
        this.otpGenerator = otpGenerator;
    }

    @Override
    @Transactional
    public String generateToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        String otpCode = otpGenerator.generateTimeCode();

        return saveTokenEntity(user, token, otpCode, 15);
    }
}
