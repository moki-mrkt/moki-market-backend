package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;
import ua.moki.util.exceptions.InvalidTokenException;

import java.text.ParseException;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenJwsStringDeserializer implements Function<String, Token> {

    private final JWSVerifier jwsVerifier;

    @Override
    public Token apply(String tokenString) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenString);

            if (!signedJWT.verify(jwsVerifier)) {
                log.warn("JWT signature verification failed for token: {}", tokenString);
                throw new InvalidTokenException("Invalid token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime().toInstant().isBefore(java.time.Instant.now())) {
                log.warn("Token expired for user: {}", claims.getSubject());
                throw new InvalidTokenException("Token expired");
            }

            return new Token(
                    claims.getSubject(),
                    claims.getStringListClaim("authorities"),
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant()
            );

        } catch (ParseException | JOSEException e) {
            log.error("Error decoding JWT: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token format");
        }
    }
}
