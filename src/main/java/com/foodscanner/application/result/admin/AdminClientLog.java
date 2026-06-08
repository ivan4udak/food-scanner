package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Запись клиентского лога для просмотра в админке (с полным контекстом).
 */
public record AdminClientLog(
        UUID id,
        UUID contributorId,
        String username,
        UUID sessionId,
        UUID correlationId,
        Instant timestamp,
        String level,
        String category,
        String event,
        String screen,
        String message,
        String metadataJson,
        Long durationMs,
        String stackTrace,
        String barcode,
        String apiMethod,
        String apiPath,
        Integer httpStatus
) {}
