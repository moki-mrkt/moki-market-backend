package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.moki.modules.users.security.Token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class AccessTokenJwsStringSerializerTest {

    private static final String SECRET = "correct-super-secret-key-must-be-32-chars-long";

    @Test
    @DisplayName("apply successfully serializes Token into a valid JWT string")
    void apply_shouldReturnValidJwtString_whenTokenIsValid() throws Exception {

        JWSSigner signer = new MACSigner(SECRET);
        AccessTokenJwsStringSerializer serializer = new AccessTokenJwsStringSerializer(signer);

        String subject = "user-123";
        List<String> authorities = List.of("ROLE_USER", "ROLE_ADMIN");
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        Token token = new Token(
                subject,
                authorities,
                now,
                expiresAt
        );

        String tokenString = serializer.apply(token);

        assertThat(tokenString).isNotNull().isNotEmpty();

        SignedJWT parsedJwt = SignedJWT.parse(tokenString);

        assertThat(parsedJwt.verify(new MACVerifier(SECRET))).isTrue();

        assertThat(parsedJwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);

        JWTClaimsSet claims = parsedJwt.getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.getStringListClaim("authorities")).containsExactlyElementsOf(authorities);
        assertThat(claims.getJWTID()).isNotNull();

        assertThat(claims.getIssueTime().toInstant()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(claims.getExpirationTime().toInstant()).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("apply throws IllegalStateException when a signature error occurs (JOSEException)")
    void apply_shouldThrowException_whenSigningFails() throws Exception {

        JWSSigner mockSigner = mock(JWSSigner.class);
        AccessTokenJwsStringSerializer serializer = new AccessTokenJwsStringSerializer(mockSigner);

        Token token = new Token(
                "user-error",
                List.of(),
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        doThrow(new JOSEException("Signing failed")).when(mockSigner).sign(any(), any());

        assertThatThrownBy(() -> serializer.apply(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not sign JWT token")
                .hasCauseInstanceOf(JOSEException.class);
    }

}
