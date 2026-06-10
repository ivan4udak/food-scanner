package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Строка списка OCR-задач в админке (barcode резолвится через draft/entry).
 */
public record AdminOcrRow(
        UUID jobId,
        String barcode,
        UUID draftId,
        UUID catalogEntryId,
        String photoType,
        String storageKey,
        int statusCode,
        String status,
        int attempts,
        Instant updatedAt,
        String errorCode,
        String errorMessage,
        String rawTextPreview
) {}
