package ua.moki.configuration;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Configuration
public class JwtCryptoConfig {

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.secret}")
    private String secret;

//    @Bean
//    public JwtFilter jwtFilter(
//            HandlerExceptionResolver handlerExceptionResolver,
//            AccessTokenJwsStringDeserializer accessTokenDeserializer
//    ) {
//        return new JwtFilter(handlerExceptionResolver, accessTokenDeserializer);
//    }


    @Bean
    public JWEEncrypter jweEncrypter() throws KeyLengthException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] secretBytes = digest.digest(refreshSecret.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = new byte[16];
        System.arraycopy(secretBytes, 0, keyBytes, 0, 16);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        return new DirectEncrypter(secretKey);
    }

    @Bean
    public JWEDecrypter jweDecrypter() throws KeyLengthException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] secretBytes = digest.digest(refreshSecret.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = new byte[16];
        System.arraycopy(secretBytes, 0, keyBytes, 0, 16);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        return new DirectDecrypter(secretKey);
    }

    @Bean
    public JWSSigner jwsSigner() throws KeyLengthException {
        return new MACSigner(secret);
    }

    @Bean
    public JWSVerifier jwsVerifier() throws JOSEException {
        return new MACVerifier(secret);
    }
}
