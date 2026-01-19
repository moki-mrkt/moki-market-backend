package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.moki.modules.users.security.Token;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class RefreshTokenJweStringSerializeTest {

    @Test
    @DisplayName("apply successfully creates and encrypts Refresh Token")
     void apply_shouldReturnValidEncryptedToken_whenTokenIsValid() throws Exception {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();

        JWEEncrypter realEncrypter = new DirectEncrypter(secretKey);
        RefreshTokenJweStringSerializer serializer = new RefreshTokenJweStringSerializer(realEncrypter);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.DAYS);
        List<String> authorities = List.of("JWT_REFRESH", "ROLE_USER");
        String subject = "user-uuid-123";

        Token token = new Token(subject, authorities, now, expiresAt);

        String serializedToken = serializer.apply(token);

        assertThat(serializedToken).isNotNull().isNotEmpty();

        EncryptedJWT encryptedJWT = EncryptedJWT.parse(serializedToken);
        JWEDecrypter decrypter = new DirectDecrypter(secretKey);

        encryptedJWT.decrypt(decrypter);

        JWTClaimsSet claims = encryptedJWT.getJWTClaimsSet();

        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.getStringListClaim("authorities")).containsExactlyElementsOf(authorities);
        assertThat(claims.getJWTID()).isNotNull();

        assertThat(encryptedJWT.getHeader().getAlgorithm()).isEqualTo(JWEAlgorithm.DIR);
        assertThat(encryptedJWT.getHeader().getEncryptionMethod()).isEqualTo(EncryptionMethod.A128GCM);
        assertThat(encryptedJWT.getHeader().getContentType()).isEqualTo("JWT");

        assertThat(claims.getIssueTime().toInstant()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(claims.getExpirationTime().toInstant()).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("apply throws IllegalStateException on encryption error")
    void apply_shouldThrowException_whenEncryptionFails() throws Exception {

        JWEEncrypter mockEncrypter = mock(JWEEncrypter.class);
        RefreshTokenJweStringSerializer serializer = new RefreshTokenJweStringSerializer(mockEncrypter);

        Token token = new Token(
                "user-test",
                List.of(),
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        doThrow(new JOSEException("Mock encryption failure"))
                .when(mockEncrypter).encrypt(any(), any(), any());

        assertThatThrownBy(() -> serializer.apply(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not encrypt Refresh Token")
                .hasCauseInstanceOf(JOSEException.class);
    }
}
