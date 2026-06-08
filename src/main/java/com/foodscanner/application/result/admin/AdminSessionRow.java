package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Клиентская сессия пользователя (для карточки пользователя).
 */
public record AdminSessionRow(
        UUID sessionId,
        Instant startedAt,
        Instant lastSeenAt,
        String clientVersion,
        String browser,
        String os,
        String deviceType,
        String networkStatus,
        Boolean standalone
) {}
