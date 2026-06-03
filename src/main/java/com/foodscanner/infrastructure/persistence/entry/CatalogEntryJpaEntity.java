package com.foodscanner.infrastructure.persistence.entry;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "food_catalog", name = "catalog_entries")
public class CatalogEntryJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(name = "draft_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID draftId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entry_id")
    private List<CatalogEntryPhotoJpaEntity> photos = new ArrayList<>();

    protected CatalogEntryJpaEntity() {}

    public CatalogEntryJpaEntity(UUID id, String barcode, UUID contributorId,
                                 UUID draftId, Instant createdAt, Instant updatedAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.draftId       = draftId;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    public UUID                          getId()            { return id; }
    public String                        getBarcode()       { return barcode; }
    public UUID                          getContributorId() { return contributorId; }
    public UUID                          getDraftId()       { return draftId; }
    public Instant                       getCreatedAt()     { return createdAt; }
    public Instant                       getUpdatedAt()     { return updatedAt; }
    public List<CatalogEntryPhotoJpaEntity> getPhotos()     { return photos; }

    public void setPhotos(List<CatalogEntryPhotoJpaEntity> photos) { this.photos = photos; }
}
