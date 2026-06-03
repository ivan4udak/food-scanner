package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AddDraftPhotoRequest {

    @NotNull(message = "ContributorId must not be null")
    private UUID contributorId;

    @NotBlank(message = "PhotoType must not be blank")
    private String photoType;

    @NotBlank(message = "StorageKey must not be blank")
    private String storageKey;

    public AddDraftPhotoRequest() {}
    public AddDraftPhotoRequest(UUID contributorId, String photoType, String storageKey) {
        this.contributorId = contributorId;
        this.photoType     = photoType;
        this.storageKey    = storageKey;
    }

    public UUID   getContributorId()           { return contributorId; }
    public String getPhotoType()               { return photoType; }
    public String getStorageKey()              { return storageKey; }
    public void   setContributorId(UUID v)     { this.contributorId = v; }
    public void   setPhotoType(String v)       { this.photoType = v; }
    public void   setStorageKey(String v)      { this.storageKey = v; }
}
