package com.foodscanner.application.result.me;

import java.time.Instant;

/**
 * Слой: application (результат).
 * Состояние структурного извлечения слота фото скана (по photoType OCR-источника) для клиента.
 * statusCode 0–5 (см. docs/OCR.md). Поля name/brand/manufacturer — когда извлечено (STRUCTURED).
 */
public record MeScanExtraction(
        String photoType,
        int statusCode,
        String status,
        String name,
        String brand,
        String manufacturer,
        Instant updatedAt
) {}
