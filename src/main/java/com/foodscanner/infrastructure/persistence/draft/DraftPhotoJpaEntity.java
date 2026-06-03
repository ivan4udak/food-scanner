package com.foodscanner.infrastructure.persistence.draft;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "food_catalog", name = "draft_photos")
public class DraftPhotoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "draft_id", nullable = false, columnDefinition = "uuid")
    private UUID draftId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DraftPhotoJpaEntity() {}

    public DraftPhotoJpaEntity(UUID id, UUID draftId, String type,
                               String storageKey, Instant createdAt, Instant updatedAt) {
        this.id         = id;
        this.draftId    = draftId;
        this.type       = type;
        this.storageKey = storageKey;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
    }

    public UUID    getId()         { return id; }
    public UUID    getDraftId()    { return draftId; }
    public String  getType()       { return type; }
    public String  getStorageKey() { return storageKey; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}
