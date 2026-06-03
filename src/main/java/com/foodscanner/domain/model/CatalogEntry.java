package com.foodscanner.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Aggregate Root
 *
 * Зачем: завершённая запись каталога. Результат работы контрибьютора.
 * Источник истины MVP: штрихкод + пять фотографий + автор.
 *
 * Нет name, manufacturer, status, dataSource — это данные Этапа 3 (LLM).
 *
 * Immutability: все поля final — CatalogEntry не изменяется после создания.
 * Модерация (Этап 5) будет отдельным агрегатом CatalogReview.
 *
 * Расширение:
 *   Этап 3 — из CatalogEntry создаётся Product после OCR+LLM.
 *   Этап 5 — CatalogReview ссылается на CatalogEntry.id.
 */
public final class CatalogEntry {

    private final UUID                    id;
    private final Barcode                 barcode;
    private final UUID                    contributorId;
    private final UUID                    draftId;
    private final List<CatalogEntryPhoto> photos;
    private final Instant                 createdAt;
    private final Instant                 updatedAt;

    private CatalogEntry(
            UUID id, Barcode barcode, UUID contributorId, UUID draftId,
            List<CatalogEntryPhoto> photos, Instant createdAt, Instant updatedAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.draftId       = draftId;
        this.photos        = new ArrayList<>(photos);
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    /**
     * Создаётся только из CatalogCompletionPolicy.
     * Package-private — внешний код не создаёт CatalogEntry напрямую.
     *
     * @param storageKeyByType последний storageKey каждого обязательного типа
     */
    public static CatalogEntry create(
            Barcode barcode,
            UUID contributorId,
            UUID draftId,
            Map<PhotoType, String> storageKeyByType) {
        Objects.requireNonNull(barcode,          "barcode must not be null");
        Objects.requireNonNull(contributorId,    "contributorId must not be null");
        Objects.requireNonNull(draftId,          "draftId must not be null");
        Objects.requireNonNull(storageKeyByType, "storageKeyByType must not be null");

        UUID    entryId = UUID.randomUUID();
        Instant now     = Instant.now();

        List<CatalogEntryPhoto> photos = new ArrayList<>();
        storageKeyByType.forEach((type, storageKey) ->
            photos.add(new CatalogEntryPhoto(entryId, type, storageKey))
        );

        return new CatalogEntry(entryId, barcode, contributorId, draftId, photos, now, now);
    }

    /** Восстановление из хранилища. Используется только в CatalogEntryRepositoryAdapter. */
    public static CatalogEntry reconstitute(
            UUID id, Barcode barcode, UUID contributorId, UUID draftId,
            List<CatalogEntryPhoto> photos, Instant createdAt, Instant updatedAt) {
        Objects.requireNonNull(id,            "id must not be null");
        Objects.requireNonNull(barcode,       "barcode must not be null");
        Objects.requireNonNull(contributorId, "contributorId must not be null");
        Objects.requireNonNull(draftId,       "draftId must not be null");
        Objects.requireNonNull(photos,        "photos must not be null");
        Objects.requireNonNull(createdAt,     "createdAt must not be null");
        Objects.requireNonNull(updatedAt,     "updatedAt must not be null");
        return new CatalogEntry(id, barcode, contributorId, draftId, photos, createdAt, updatedAt);
    }

    public UUID    getId()            { return id; }
    public Barcode getBarcode()       { return barcode; }
    public UUID    getContributorId() { return contributorId; }
    public UUID    getDraftId()       { return draftId; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getUpdatedAt()     { return updatedAt; }

    public List<CatalogEntryPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogEntry other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "CatalogEntry{id=" + id + ", barcode=" + barcode + "}";
    }
}
