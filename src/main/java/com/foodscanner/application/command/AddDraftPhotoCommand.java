package com.foodscanner.application.command;

import com.foodscanner.domain.model.PhotoType;
import java.util.UUID;

public final class AddDraftPhotoCommand {
    private final UUID      draftId;
    private final UUID      contributorId;
    private final PhotoType photoType;
    private final String    storageKey;

    public AddDraftPhotoCommand(UUID draftId, UUID contributorId,
                                PhotoType photoType, String storageKey) {
        this.draftId       = draftId;
        this.contributorId = contributorId;
        this.photoType     = photoType;
        this.storageKey    = storageKey;
    }

    public UUID      getDraftId()       { return draftId; }
    public UUID      getContributorId() { return contributorId; }
    public PhotoType getPhotoType()     { return photoType; }
    public String    getStorageKey()    { return storageKey; }
}
