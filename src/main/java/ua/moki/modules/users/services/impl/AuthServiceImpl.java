package ua.moki.modules.users.services.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.security.Token;
import ua.moki.modules.users.dtos.auth.AuthResponseDTO;
import ua.moki.modules.users.dtos.auth.LoginRequestDTO;
import ua.moki.modules.users.dtos.auth.RefreshTokenRequestDTO;
import ua.moki.modules.users.security.factories.DefaultAccessTokenFactory;
import ua.moki.modules.users.security.factories.DefaultRefreshTokenFactory;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringSerializer;
import ua.moki.modules.users.security.jwt.RefreshTokenJweStringSerializer;
import ua.moki.modules.users.services.AuthService;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    Duration accesstTokenTtl;

    UserRepository userRepository;
    RefreshTokenService refreshTokenService;

    AuthenticationManager authenticationManager;
    DefaultRefreshTokenFactory refreshTokenFactory;
    DefaultAccessTokenFactory accessTokenFactory;
    AccessTokenJwsStringSerializer accessTokenSerializer;
    RefreshTokenJweStringSerializer refreshTokenSerializer;

    @Autowired
    public AuthServiceImpl(@Value("${jwt.access.ttl}") Duration accessTokenTtl,
                           UserRepository userRepository,
                           RefreshTokenService refreshTokenService,
                           AuthenticationManager authenticationManager,
                           DefaultRefreshTokenFactory refreshTokenFactory,
                           DefaultAccessTokenFactory accessTokenFactory,
                           AccessTokenJwsStringSerializer accessTokenSerializer,
                           RefreshTokenJweStringSerializer refreshTokenSerializer) {
        this.accesstTokenTtl = accessTokenTtl;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenFactory = refreshTokenFactory;
        this.accessTokenFactory = accessTokenFactory;
        this.accessTokenSerializer = accessTokenSerializer;
        this.refreshTokenSerializer = refreshTokenSerializer;
    }


    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return issueTokens(authentication, user);
    }

    @Override
    public AuthResponseDTO refreshAccessToken(RefreshTokenRequestDTO request) {

        User userFromRefreshToken = refreshTokenService.consumeRefreshTokenAndGetUser(request.refreshToken());

        var authentication = new UsernamePasswordAuthenticationToken(
                userFromRefreshToken.getPublicId().toString(),
                null,
                List.of(new SimpleGrantedAuthority(userFromRefreshToken.getRoleType().name()))
        );

        return issueTokens(authentication, userFromRefreshToken);
    }

    @Override
    public void logout(String refreshTokenString) {
         refreshTokenService.deleteRefreshToken(refreshTokenString);
    }

    private AuthResponseDTO issueTokens(Authentication authentication, User userFromRefreshToken) {

        Token newRefreshTokenObj = refreshTokenFactory.apply(authentication);
        Token newAccessTokenObj = accessTokenFactory.apply(newRefreshTokenObj);

        String newRefreshTokenString = refreshTokenSerializer.apply(newRefreshTokenObj);
        String newAccessTokenString = accessTokenSerializer.apply(newAccessTokenObj);

        refreshTokenService.saveRefreshToken(newRefreshTokenString, newRefreshTokenObj, userFromRefreshToken);

        return new AuthResponseDTO(
                newAccessTokenString,
                accesstTokenTtl.getSeconds(),
                newRefreshTokenString,
                "Bearer"
        );
    }
}
