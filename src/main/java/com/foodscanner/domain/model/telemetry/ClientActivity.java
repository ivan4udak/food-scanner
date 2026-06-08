package com.foodscanner.domain.model.telemetry;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Value Object.
 *
 * Лёгкое heartbeat-событие активности клиента (для online/last-activity).
 * Не путать с потоком health-check логов — это отдельная компактная модель.
 */
public record ClientActivity(
        UUID id,
        UUID contributorId,
        UUID sessionId,
        String screen,
        Boolean online,
        Instant occurredAt,
        Instant receivedAt
) {}
