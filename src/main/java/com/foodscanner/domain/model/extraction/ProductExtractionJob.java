package com.foodscanner.domain.model.extraction;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: domain (Value Object). Задача структурного извлечения продукта.
 * На создании несёт только источник/тип/статус; структурный результат пишет worker (след. срез).
 */
public record ProductExtractionJob(
        UUID id,
        UUID ocrJobId,
        String barcode,
        ExtractionType type,
        ExtractionStatus status,
        int attempts,
        Instant queuedAt,
        Instant createdAt,
        Instant updatedAt
) {
    /** Новая задача в очереди (QUEUED) для OCR-задачи. */
    public static ProductExtractionJob queued(UUID ocrJobId, String barcode, ExtractionType type) {
        Instant now = Instant.now();
        return new ProductExtractionJob(UUID.randomUUID(), ocrJobId, barcode, type,
            ExtractionStatus.QUEUED, 0, now, now, now);
    }
}
