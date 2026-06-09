package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Скан пользователя (черновик/запись каталога).
 */
public record AdminScanRow(
        String barcode,
        String status,         // DRAFT_OPEN | COMPLETED | EXISTS
        UUID draftId,
        UUID catalogEntryId,
        Instant firstScannedAt,
        Instant completedAt,
        long photoCount
) {}
