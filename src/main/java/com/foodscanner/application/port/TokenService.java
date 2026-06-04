package com.foodscanner.application.port;

import java.util.UUID;

/**
 * Слой: application (порт)
 * Выпуск и проверка access-токенов (JWT). Реализация — в infrastructure.
 */
public interface TokenService {

    /** Выпускает access-токен (срок жизни задаётся реализацией, по умолчанию 24ч). */
    String issueAccessToken(UUID contributorId, String username);

    /** Проверяет access-токен; бросает исключение, если невалиден/просрочен. */
    AccessClaims verifyAccessToken(String token);

    record AccessClaims(UUID contributorId, String username) {}
}
