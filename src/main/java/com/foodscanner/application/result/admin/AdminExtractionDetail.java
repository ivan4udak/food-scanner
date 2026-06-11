package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Полная карточка задачи извлечения для админки + вложенный срез OCR-источника.
 */
public record AdminExtractionDetail(
        UUID id,
        UUID ocrJobId,
        String barcode,
        String type,
        int statusCode,
        String status,
        String source,
        int attempts,
        Instant queuedAt,
        Instant startedAt,      // нет отдельной колонки — всегда null (резерв)
        Instant processedAt,
        Instant updatedAt,
        String lastError,
        // структурный результат (заполняет worker/extractor; пока обычно null)
        String name,
        String brand,
        String manufacturer,
        String composition,
        String nutrition,       // raw JSONB
        String confidence,      // raw JSONB
        Boolean needsReview,
        Ocr ocr                 // null, если OCR-задача-источник не найдена
) {
    /** Срез OCR-задачи-источника (raw text + статус) для контекста извлечения. */
    public record Ocr(
            UUID jobId,
            String photoType,
            int statusCode,
            String status,
            Double confidence,
            String rawText,
            int rawTextLength,
            String storageKey,
            String errorCode,
            String errorMessage
    ) {}
}
