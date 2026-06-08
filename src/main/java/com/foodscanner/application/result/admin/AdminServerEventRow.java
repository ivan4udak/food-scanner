package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Серверное событие для просмотра в админке.
 */
public record AdminServerEventRow(
        UUID id,
        Instant occurredAt,
        String level,
        String event,
        UUID correlationId,
        UUID contributorId,
        String username,
        String method,
        String path,
        Integer httpStatus,
        Long durationMs,
        String useCase,
        String barcode,
        String errorCode,
        String errorMessage,
        String exceptionClass
) {}
