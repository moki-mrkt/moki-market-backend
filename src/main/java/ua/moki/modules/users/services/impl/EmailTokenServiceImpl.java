package ua.moki.modules.users.services.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.moki.modules.users.dtos.EmailChangeClaimsDTO;
import ua.moki.modules.users.services.EmailTokenService;
import ua.moki.util.exceptions.InvalidTokenException;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTokenServiceImpl  implements EmailTokenService {

    private final JWSSigner jwsSigner;
    @Value("${jwt.secret}")
    private String secret;

    @Override
    public String generateEmailChangeToken(UUID userId, String newEmail) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("new_email", newEmail) // Нова пошта
                    .claim("type", "EMAIL_CHANGE")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 хвилин
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(jwsSigner);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error creating email token", e);
        }
    }

    @Override
    public EmailChangeClaimsDTO parseToken(String tokenString) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenString);

            if (!signedJWT.verify(new MACVerifier(secret))) {
                throw new InvalidTokenException("Invalid token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime().before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            if (!"EMAIL_CHANGE".equals(claims.getStringClaim("type"))) {
                throw new InvalidTokenException("Invalid token type");
            }

            return new EmailChangeClaimsDTO(
                    UUID.fromString(claims.getSubject()),
                    claims.getStringClaim("new_email")
            );

        } catch (JOSEException | ParseException e) {
            throw new InvalidTokenException("Invalid token");
        }
    }
}
