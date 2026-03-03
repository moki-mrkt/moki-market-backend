package ua.moki.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class OtpGenerator {

    private final SecureRandom secureRandom;

    @Autowired
    public OtpGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generateTimeCode() {
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d", code);
    }
}
