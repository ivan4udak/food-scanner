package com.foodscanner.application.result.me;

import java.time.Instant;

/**
 * Слой: application (результат).
 * OCR-состояние слота фото скана (по photoType) для клиента «Мои сканы».
 * statusCode 0–5 (см. docs/OCR.md); клиент сам решает, что показывать (rawText — опционально).
 */
public record MeScanOcr(
        String photoType,
        int statusCode,
        String status,
        Double confidence,
        Instant updatedAt,
        String errorCode,
        String errorMessage,
        String rawTextPreview
) {}
