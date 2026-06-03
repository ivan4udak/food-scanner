package com.foodscanner.application.port;

/**
 * Слой: application (порт)
 * Абстракция хеширования паролей. Реализация (BCrypt) — в infrastructure.
 * Домен и use case не знают про конкретный алгоритм.
 */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String passwordHash);
}
