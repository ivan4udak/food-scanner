package com.foodscanner.application.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (команда).
 * Лёгкое событие активности клиента.
 */
public record RecordClientActivityCommand(
        UUID contributorId,
        UUID sessionId,
        String screen,
        Boolean online,
        Instant occurredAt
) {}
