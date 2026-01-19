package ua.moki.modules.users.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.RefreshToken;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.RefreshTokenRepository;
import ua.moki.modules.users.security.Token;
import ua.moki.modules.users.security.jwt.RefreshTokenJweStringDeserializer;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.Instant;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenJweStringDeserializer refreshTokenDeserializer;

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenJweStringDeserializer refreshTokenDeserializer,
                                   RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenDeserializer = refreshTokenDeserializer;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public void saveRefreshToken(String newRefreshTokenString, Token newRefreshTokenObj, User user) {

        RefreshToken newRefreshToken = new RefreshToken();

        newRefreshToken.setToken(newRefreshTokenString);
        newRefreshToken.setUser(user);
        newRefreshToken.setExpiryDate(newRefreshTokenObj.expiresAt());

        refreshTokenRepository.save(newRefreshToken);
    }

    @Override
    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteAllForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public User consumeRefreshTokenAndGetUser(String oldRefreshTokenString) {
        Token decodedOldToken = refreshTokenDeserializer.apply(oldRefreshTokenString);

        if (decodedOldToken == null) {
            throw new InvalidTokenException("Invalid or expired refresh token format");
        }

        RefreshToken savedTokenEntity = refreshTokenRepository.findByToken(oldRefreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Token not found or already used"));

        if (savedTokenEntity.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(savedTokenEntity);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = savedTokenEntity.getUser();

        refreshTokenRepository.delete(savedTokenEntity);

        return user;
    }
}
