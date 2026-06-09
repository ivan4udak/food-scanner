package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Детальная карточка записи каталога: фото + связанные клиентские логи.
 */
public record AdminCatalogDetail(
        UUID catalogEntryId,
        String barcode,
        UUID contributorId,
        String author,
        Instant createdAt,
        List<Photo> photos,
        List<AdminClientLog> relatedLogs
) {
    public record Photo(UUID id, String type, String storageKey, Instant capturedAt) {}
}
