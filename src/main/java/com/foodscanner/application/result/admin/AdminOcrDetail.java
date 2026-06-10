package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Полная карточка OCR-задачи для админки (включая полный rawText/parsed + lifecycle/publish).
 */
public record AdminOcrDetail(
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
        Double confidence,
        Instant createdAt,
        Instant updatedAt,
        String errorCode,
        String errorMessage,
        String rawText,
        String parsedIngredients,
        String parsedNutrition,
        Instant publishedAt,
        int publishAttempts,
        String lastPublishError,
        Instant supersededAt,
        UUID supersededBy
) {}
