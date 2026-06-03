package com.foodscanner.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CatalogEntryResponse {

    private UUID         id;
    private String       barcode;
    private UUID         contributorId;
    private List<PhotoDto> photos;
    private Instant      createdAt;

    public static class PhotoDto {
        private UUID   id;
        private String type;
        private String storageKey;

        public PhotoDto() {}
        public PhotoDto(UUID id, String type, String storageKey) {
            this.id         = id;
            this.type       = type;
            this.storageKey = storageKey;
        }

        public UUID   getId()                  { return id; }
        public String getType()                { return type; }
        public String getStorageKey()          { return storageKey; }
        public void   setId(UUID v)            { this.id = v; }
        public void   setType(String v)        { this.type = v; }
        public void   setStorageKey(String v)  { this.storageKey = v; }
    }

    public CatalogEntryResponse() {}
    public CatalogEntryResponse(UUID id, String barcode, UUID contributorId,
                                List<PhotoDto> photos, Instant createdAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.photos        = photos;
        this.createdAt     = createdAt;
    }

    public UUID           getId()                   { return id; }
    public String         getBarcode()              { return barcode; }
    public UUID           getContributorId()        { return contributorId; }
    public List<PhotoDto> getPhotos()               { return photos; }
    public Instant        getCreatedAt()            { return createdAt; }
    public void           setId(UUID v)             { this.id = v; }
    public void           setBarcode(String v)      { this.barcode = v; }
    public void           setContributorId(UUID v)  { this.contributorId = v; }
    public void           setPhotos(List<PhotoDto> v){ this.photos = v; }
    public void           setCreatedAt(Instant v)   { this.createdAt = v; }
}
