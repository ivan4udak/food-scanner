package com.foodscanner.domain.model.ocr;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: domain (Value Object). OCR-задача по фото (INGREDIENTS/NUTRITION).
 * Результаты заполняются OCR-сервисом (следующий срез).
 */
public record OcrJob(
        UUID id,
        UUID draftId,
        UUID catalogEntryId,
        String storageKey,
        String photoType,
        OcrStatus status,
        int attempts,
        String rawText,
        String parsedIngredients,
        String parsedNutrition,
        Double confidence,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    /** Новая задача в очереди (QUEUED) для фото черновика. */
    public static OcrJob queued(UUID draftId, String storageKey, String photoType) {
        Instant now = Instant.now();
        return new OcrJob(UUID.randomUUID(), draftId, null, storageKey, photoType,
            OcrStatus.QUEUED, 0, null, null, null, null, null, null, now, now);
    }
}
