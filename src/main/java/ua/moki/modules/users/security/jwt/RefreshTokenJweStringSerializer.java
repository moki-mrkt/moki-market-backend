package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;

import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenJweStringSerializer implements Function<Token, String> {

    private final JWEEncrypter jweEncrypter;
    private final JWEAlgorithm jweAlgorithm = JWEAlgorithm.DIR;
    private final EncryptionMethod encryptionMethod = EncryptionMethod.A128GCM;

    @Override
    public String apply(Token token) {

        JWEHeader header = new JWEHeader.Builder(jweAlgorithm, encryptionMethod)
                .contentType("JWT")
                .build();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(token.subject())
                .issueTime(Date.from(token.createdAt()))
                .expirationTime(Date.from(token.expiresAt()))
                .claim("authorities", token.authorities())
                .build();

        EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

        try {
            encryptedJWT.encrypt(jweEncrypter);
            return encryptedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error encrypting Refresh Token: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not encrypt Refresh Token", e);
        }
    }
}