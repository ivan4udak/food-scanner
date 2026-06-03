package com.foodscanner.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Entity, owned by CatalogEntry
 *
 * Зачем: постоянная запись о фото в завершённом каталоге.
 * Отличается от DraftPhoto неизменяемостью после создания —
 * фото записи каталога не заменяются (только через модерацию в Этапе 5).
 *
 * Инкапсуляция: конструктор package-private — создаётся только
 * через CatalogCompletionPolicy.createEntry().
 *
 * Immutability: полная.
 *
 * Расширение: добавить verificationStatus (PENDING/APPROVED/REJECTED)
 * в Этапе 5 при появлении модерации CatalogReview.
 */
public final class CatalogEntryPhoto {

    private final UUID      id;
    private final UUID      entryId;
    private final PhotoType type;
    private final String    storageKey;
    private final Instant   createdAt;

    CatalogEntryPhoto(UUID entryId, PhotoType type, String storageKey) {
        this.id         = UUID.randomUUID();
        this.entryId    = entryId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = Instant.now();
    }

    /** Восстановление из хранилища. Используется только в CatalogEntryRepositoryAdapter. */
    public CatalogEntryPhoto(UUID id, UUID entryId, PhotoType type,
                             String storageKey, Instant createdAt) {
        this.id         = id;
        this.entryId    = entryId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = createdAt;
    }

    public UUID      getId()         { return id; }
    public UUID      getEntryId()    { return entryId; }
    public PhotoType getType()       { return type; }
    public String    getStorageKey() { return storageKey; }
    public Instant   getCreatedAt()  { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogEntryPhoto other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
