package com.foodscanner.application.result.me;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Детали скана пользователя: фото с готовыми URL (thumb/full).
 * ocrStatus — задел под v1.10 (пока null).
 */
public record MeScanDetail(
        String barcode,
        UUID catalogEntryId,
        Instant firstScannedAt,
        Instant completedAt,
        List<Photo> photos,
        List<MeScanOcr> ocr,
        List<MeScanExtraction> extraction,
        String ocrStatus
) {
    public record Photo(
            UUID id,
            String type,
            String storageKey,
            String thumbUrl,
            String fullUrl,
            Instant capturedAt
    ) {}
}
