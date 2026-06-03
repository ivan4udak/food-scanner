package com.foodscanner.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Entity, owned by CatalogDraft
 *
 * Зачем: временное фото черновика. Отдельная сущность от CatalogEntryPhoto
 * потому что у них разные жизненные циклы — фото черновика можно заменить,
 * фото записи каталога постоянны.
 *
 * Инкапсуляция: конструктор package-private — создаётся только через
 * CatalogDraft.addPhoto().
 *
 * Immutability: полная, все поля final.
 */
public final class DraftPhoto {

    private final UUID      id;
    private final UUID      draftId;
    private final PhotoType type;
    private final String    storageKey;
    private final Instant   createdAt;

    DraftPhoto(UUID draftId, PhotoType type, String storageKey) {
        this.id         = UUID.randomUUID();
        this.draftId    = draftId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = Instant.now();
    }

    /** Восстановление из хранилища. Используется только в CatalogDraftRepositoryAdapter. */
    public DraftPhoto(UUID id, UUID draftId, PhotoType type, String storageKey, Instant createdAt) {
        this.id         = id;
        this.draftId    = draftId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = createdAt;
    }

    public UUID      getId()         { return id; }
    public UUID      getDraftId()    { return draftId; }
    public PhotoType getType()       { return type; }
    public String    getStorageKey() { return storageKey; }
    public Instant   getCreatedAt()  { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DraftPhoto other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
