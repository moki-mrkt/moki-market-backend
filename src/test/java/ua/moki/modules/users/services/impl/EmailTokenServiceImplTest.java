package ua.moki.modules.users.services.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ua.moki.modules.users.dtos.EmailChangeClaimsDTO;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class EmailTokenServiceImplTest {

    private EmailTokenServiceImpl emailTokenService;

    private static final String SECRET = "test-secret-key-must-be-very-long-and-secure";
    private static final String WRONG_SECRET = "wrong-secret-key-must-be-very-long-and-secure";

    @BeforeEach
    void setUp() throws Exception {

        JWSSigner jwsSigner = new MACSigner(SECRET);

        emailTokenService = new EmailTokenServiceImpl(jwsSigner);

        ReflectionTestUtils.setField(emailTokenService, "secret", SECRET);
    }

    @Test
    @DisplayName("generateEmailChangeToken creates a valid JWT with correct claims")
    void generateToken_shouldCreateValidToken() throws Exception {

        UUID userId = UUID.randomUUID();
        String newEmail = "new.email@test.com";

        String token = emailTokenService.generateEmailChangeToken(userId, newEmail);

        assertThat(token).isNotEmpty();

        SignedJWT signedJWT = SignedJWT.parse(token);
        assertThat(signedJWT.verify(new MACVerifier(SECRET))).isTrue();

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getStringClaim("new_email")).isEqualTo(newEmail);
        assertThat(claims.getStringClaim("type")).isEqualTo("EMAIL_CHANGE");

        Instant expectedExp = Instant.now().plus(15, ChronoUnit.MINUTES);
        assertThat(claims.getExpirationTime().toInstant())
                .isCloseTo(expectedExp, within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("parseToken successfully parses a valid token and returns a DTO")
    void parseToken_shouldReturnDto_whenTokenIsValid() throws Exception {

        UUID userId = UUID.randomUUID();
        String newEmail = "valid@test.com";

        String token = emailTokenService.generateEmailChangeToken(userId, newEmail);

        EmailChangeClaimsDTO result = emailTokenService.parseToken(token);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.newEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("parseToken throws InvalidTokenException if the signature is invalid")
    void parseToken_shouldThrowException_whenSignatureIsInvalid() throws Exception {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("new_email", "hacker@test.com")
                .claim("type", "EMAIL_CHANGE")
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(WRONG_SECRET));
        String forgedToken = signedJWT.serialize();

        assertThatThrownBy(() -> emailTokenService.parseToken(forgedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token signature");
    }

    @Test
    @DisplayName("parseToken throws InvalidTokenException if the token is expired")
    void parseToken_shouldThrowException_whenTokenIsExpired() throws Exception {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("new_email", "expired@test.com")
                .claim("type", "EMAIL_CHANGE")
                .expirationTime(new Date(System.currentTimeMillis() - 10000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(SECRET));
        String expiredToken = signedJWT.serialize();

        assertThatThrownBy(() -> emailTokenService.parseToken(expiredToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token expired");
    }

    @Test
    @DisplayName("parseToken throws InvalidTokenException if the token type is incorrect")
    void parseToken_shouldThrowException_whenTokenTypeIsWrong() throws Exception {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("new_email", "wrong.type@test.com")
                .claim("type", "ACCESS_TOKEN")
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(SECRET));
        String wrongTypeToken = signedJWT.serialize();

        assertThatThrownBy(() -> emailTokenService.parseToken(wrongTypeToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    @DisplayName("parseToken throws InvalidTokenException if the string format is incorrect")
    void parseToken_shouldThrowException_whenTokenIsMalformed() {

        String garbage = "not.a.valid.jwt";

        assertThatThrownBy(() -> emailTokenService.parseToken(garbage))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token");
    }
}