package com.foodscanner.application.command;

import com.foodscanner.domain.model.PhotoType;
import java.time.Instant;
import java.util.UUID;

public final class AddDraftPhotoCommand {
    private final UUID      draftId;
    private final UUID      contributorId;
    private final PhotoType photoType;
    private final String    storageKey;
    private final Instant   capturedAt;   // дата съёмки из метаданных; может быть null

    public AddDraftPhotoCommand(UUID draftId, UUID contributorId,
                                PhotoType photoType, String storageKey) {
        this(draftId, contributorId, photoType, storageKey, null);
    }

    public AddDraftPhotoCommand(UUID draftId, UUID contributorId,
                                PhotoType photoType, String storageKey, Instant capturedAt) {
        this.draftId       = draftId;
        this.contributorId = contributorId;
        this.photoType     = photoType;
        this.storageKey    = storageKey;
        this.capturedAt    = capturedAt;
    }

    public UUID      getDraftId()       { return draftId; }
    public UUID      getContributorId() { return contributorId; }
    public PhotoType getPhotoType()     { return photoType; }
    public String    getStorageKey()    { return storageKey; }
    public Instant   getCapturedAt()    { return capturedAt; }
}
