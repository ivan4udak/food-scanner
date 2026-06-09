package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Строка списка каталога в админке.
 */
public record AdminCatalogRow(
        UUID catalogEntryId,
        String barcode,
        UUID contributorId,
        String author,
        Instant createdAt,
        long photoCount,
        int qualityScore
) {}
