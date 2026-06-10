package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Строка списка OCR-задач в админке (barcode/автор резолвятся через draft/entry).
 */
public record AdminOcrRow(
        UUID jobId,
        String barcode,
        UUID contributorId,
        String author,
        UUID draftId,
        UUID catalogEntryId,
        String photoType,
        String storageKey,
        int statusCode,
        String status,
        int attempts,
        boolean active,
        boolean orphaned,
        Instant updatedAt,
        String errorCode,
        String errorMessage,
        String rawTextPreview
) {}
