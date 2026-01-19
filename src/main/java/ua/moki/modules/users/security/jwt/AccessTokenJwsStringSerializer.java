package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.moki.modules.users.security.Token;

import com.nimbusds.jose.JWSSigner;

import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenJwsStringSerializer implements Function<Token, String> {

    private final JWSSigner jwsSigner;

    private final JWSAlgorithm jwsAlgorithm = JWSAlgorithm.HS256;

    @Override
    public String apply(Token token) {

        JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
                .build();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(token.subject())
                .issueTime(Date.from(token.createdAt()))
                .expirationTime(Date.from(token.expiresAt()))
                .claim("authorities", token.authorities());

        claimsBuilder.jwtID(UUID.randomUUID().toString());

        JWTClaimsSet claimsSet = claimsBuilder.build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {
            signedJWT.sign(jwsSigner);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error signing Access Token: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not sign JWT token", e);
        }
    }
}
