package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.moki.modules.users.security.Token;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.within;

public class AccessTokenJwsStringDeserializerTest {

    private AccessTokenJwsStringDeserializer deserializer;
    private JWSSigner signer;

    private static final String SECRET = "my-super-secret-key-must-be-at-least-32-chars";
    private static final String WRONG_SECRET = "wrong-super-secret-key-must-be-at-least-32-chars";

    @BeforeEach
    void setUp() throws Exception {

        JWSVerifier verifier = new MACVerifier(SECRET);
        signer = new MACSigner(SECRET);

        deserializer = new AccessTokenJwsStringDeserializer(verifier);
    }

    @Test
    @DisplayName("apply successfully deserializes a valid token")
    void apply_shouldReturnToken_whenTokenIsValid() throws Exception {

        String subject = UUID.randomUUID().toString();
        List<String> authorities = List.of("ROLE_USER", "ROLE_ADMIN");
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("authorities", authorities)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        String tokenString = signedJWT.serialize();

        Token token = deserializer.apply(tokenString);

        assertThat(token).isNotNull();
        assertThat(token.subject()).isEqualTo(subject);
        assertThat(token.authorities()).containsExactlyElementsOf(authorities);

        assertThat(token.createdAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(token.expiresAt()).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when token is expired")
    void apply_shouldThrowException_whenTokenIsExpired() throws Exception {

        Instant now = Instant.now();
        Instant expiredAt = now.minus(1, ChronoUnit.HOURS);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user-123")
                .expirationTime(Date.from(expiredAt))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        String expiredTokenString = signedJWT.serialize();

        assertThatThrownBy(() -> deserializer.apply(expiredTokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when the signature is invalid")
    void apply_shouldThrowException_whenSignatureIsInvalid() throws Exception {

        JWSSigner wrongSigner = new MACSigner(WRONG_SECRET);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("hacker")
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(wrongSigner);
        String forgedTokenString = signedJWT.serialize();

        assertThatThrownBy(() -> deserializer.apply(forgedTokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("signature");
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when the token format is incorrect")
    void apply_shouldThrowException_whenTokenIsMalformed() {

        String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> deserializer.apply(malformedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("token format");
    }
}
