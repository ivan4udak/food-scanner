package com.foodscanner.application.result.me;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Скан пользователя для экрана «Мои сканы».
 * ocrStatus — задел под v1.10 (пока всегда null, в UI не показывается).
 */
public record MeScanRow(
        String barcode,
        String scanStatus,     // DRAFT_OPEN | COMPLETED
        UUID catalogEntryId,
        Instant firstScannedAt,
        Instant completedAt,
        long photoCount,
        String ocrStatus
) {}
