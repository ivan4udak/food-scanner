package com.foodscanner.infrastructure.persistence.entry;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "food_catalog", name = "catalog_entry_photos")
public class CatalogEntryPhotoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "entry_id", nullable = false, columnDefinition = "uuid")
    private UUID entryId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    protected CatalogEntryPhotoJpaEntity() {}

    public CatalogEntryPhotoJpaEntity(UUID id, UUID entryId, String type,
                                      String storageKey, Instant createdAt, Instant updatedAt,
                                      Instant capturedAt) {
        this.id         = id;
        this.entryId    = entryId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
        this.capturedAt = capturedAt;
    }

    public UUID    getId()         { return id; }
    public UUID    getEntryId()    { return entryId; }
    public String  getType()       { return type; }
    public String  getStorageKey() { return storageKey; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getCapturedAt() { return capturedAt; }
}
