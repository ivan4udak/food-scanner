package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Строка списка задач структурного извлечения в админке.
 */
public record AdminExtractionRow(
        UUID jobId,
        UUID ocrJobId,
        String barcode,
        String type,
        int statusCode,
        String status,
        int attempts,
        String source,
        String name,
        String brand,
        String manufacturer,
        String lastError,
        Instant queuedAt,
        Instant processedAt,
        Instant updatedAt
) {}
