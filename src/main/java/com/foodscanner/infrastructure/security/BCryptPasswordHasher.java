package com.foodscanner.infrastructure.security;

import com.foodscanner.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Слой: infrastructure
 * Реализация PasswordHasher на BCrypt (spring-security-crypto).
 */
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) return false;
        return encoder.matches(rawPassword, passwordHash);
    }
}
