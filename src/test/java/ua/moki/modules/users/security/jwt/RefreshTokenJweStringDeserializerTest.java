package ua.moki.modules.users.security.jwt;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.moki.modules.users.security.Token;
import ua.moki.util.exceptions.InvalidTokenException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;

class RefreshTokenJweStringDeserializerTest {

    private RefreshTokenJweStringDeserializer deserializer;
    private SecretKey secretKey;
    private SecretKey wrongKey;

    @BeforeEach
    void setUp() throws Exception {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        secretKey = keyGen.generateKey();
        wrongKey = keyGen.generateKey();

        JWEDecrypter decrypter = new DirectDecrypter(secretKey);

        deserializer = new RefreshTokenJweStringDeserializer(decrypter);
    }

    @Test
    @DisplayName("apply successfully decrypts and deserializes a valid JWE token")
    void apply_shouldReturnToken_whenTokenIsValid() throws Exception {

        String subject = UUID.randomUUID().toString();
        List<String> authorities = List.of("JWT_REFRESH", "ROLE_USER");
        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.DAYS);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("authorities", authorities)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();

        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
        EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

        DirectEncrypter encrypter = new DirectEncrypter(secretKey);
        encryptedJWT.encrypt(encrypter);
        String tokenString = encryptedJWT.serialize();

        Token resultToken = deserializer.apply(tokenString);

        assertThat(resultToken).isNotNull();
        assertThat(resultToken.subject()).isEqualTo(subject);
        assertThat(resultToken.authorities()).containsExactlyElementsOf(authorities);

        assertThat(resultToken.createdAt()).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        assertThat(resultToken.expiresAt()).isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when token is expired")
    void apply_shouldThrowException_whenTokenIsExpired() throws Exception {

        Instant now = Instant.now();
        Instant expiredAt = now.minus(1, ChronoUnit.DAYS);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("expired-user")
                .expirationTime(Date.from(expiredAt))
                .build();

        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
        EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

        encryptedJWT.encrypt(new DirectEncrypter(secretKey));
        String expiredTokenString = encryptedJWT.serialize();

        assertThatThrownBy(() -> deserializer.apply(expiredTokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when decryption fails (invalid key)")
    void apply_shouldThrowException_whenDecryptionFails() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("hacker")
                .build();

        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
        EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

        encryptedJWT.encrypt(new DirectEncrypter(wrongKey));
        String unreadableTokenString = encryptedJWT.serialize();

        assertThatThrownBy(() -> deserializer.apply(unreadableTokenString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    @DisplayName("apply throws InvalidTokenException when the token format is incorrect")
    void apply_shouldThrowException_whenTokenIsMalformed() {

        String garbageString = "this.is.not.a.valid.jwe.token";

        assertThatThrownBy(() -> deserializer.apply(garbageString))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token format");
    }
}
