package com.foodscanner.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (порт чтения «Мои сканы»). Реализация — нативный SQL в infrastructure.
 * Все методы — в пределах одного contributorId (пользователь видит только своё).
 */
public interface MeReadPort {

    record ScanData(
            String barcode, String status, UUID draftId, UUID catalogEntryId,
            Instant firstScannedAt, Instant completedAt, long photoCount) {}

    record PhotoData(UUID id, String type, String storageKey, Instant capturedAt) {}

    record OcrData(String photoType, int statusCode, Double confidence, Instant updatedAt,
                   String rawTextPreview, String errorCode, String errorMessage) {}

    List<ScanData> scans(UUID contributorId);

    Optional<ScanData> scan(UUID contributorId, String barcode);

    /** Фото скана: из завершённой записи (catalog_entry_photos) либо из черновика (draft_photos). */
    List<PhotoData> photos(UUID contributorId, String barcode);

    /** Активные OCR-задачи скана (по draft/entry). Владение проверяется вызывающим (scan()). */
    List<OcrData> ocrForScan(UUID draftId, UUID catalogEntryId);
}
