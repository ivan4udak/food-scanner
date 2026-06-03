package com.foodscanner.infrastructure.persistence.draft;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "food_catalog", name = "catalog_drafts")
public class CatalogDraftJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "draft_id")
    private List<DraftPhotoJpaEntity> photos = new ArrayList<>();

    protected CatalogDraftJpaEntity() {}

    public CatalogDraftJpaEntity(UUID id, String barcode, UUID contributorId,
                                 String status, Instant createdAt, Instant updatedAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.status        = status;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    public UUID                    getId()            { return id; }
    public String                  getBarcode()       { return barcode; }
    public UUID                    getContributorId() { return contributorId; }
    public String                  getStatus()        { return status; }
    public Instant                 getCreatedAt()     { return createdAt; }
    public Instant                 getUpdatedAt()     { return updatedAt; }
    public List<DraftPhotoJpaEntity> getPhotos()      { return photos; }

    public void setStatus(String status)   { this.status    = status; }
    public void setUpdatedAt(Instant t)    { this.updatedAt = t; }
    public void setPhotos(List<DraftPhotoJpaEntity> photos) { this.photos = photos; }
}
