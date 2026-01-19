package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;
import ua.moki.util.exceptions.InvalidTokenException;

import java.text.ParseException;
import java.time.Instant;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenJweStringDeserializer implements Function<String, Token> {

    private final JWEDecrypter jweDecrypter;

    @Override
    public Token apply(String tokenString) {
        try {
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(tokenString);

            encryptedJWT.decrypt(jweDecrypter);

            JWTClaimsSet claims = encryptedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                log.warn("Refresh Token expired for user: {}", claims.getSubject());
                throw new InvalidTokenException("Refresh token expired");
            }

            return new Token(
                    claims.getSubject(),
                    claims.getStringListClaim("authorities"),
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant()
            );

        } catch (ParseException | JOSEException e) {
            log.error("Failed to decrypt Refresh Token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid refresh token format or decryption failed");
        }
    }
}
