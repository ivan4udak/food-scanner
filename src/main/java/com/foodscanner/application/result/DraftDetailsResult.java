package com.foodscanner.application.result;

import com.foodscanner.domain.model.PhotoType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Слой: application
 * Состояние черновика для восстановления на клиенте (GET /drafts/{id}).
 * photos — эффективный набор (по одному, последнему, фото на тип).
 */
public final class DraftDetailsResult {

    public record Photo(PhotoType type, String storageKey, Instant capturedAt) {}

    private final UUID            draftId;
    private final String          barcode;
    private final String          status;
    private final List<Photo>     photos;
    private final int             uploadedCount;
    private final int             requiredCount;
    private final Set<PhotoType>  missingTypes;
    private final boolean         complete;

    public DraftDetailsResult(UUID draftId, String barcode, String status, List<Photo> photos,
                              int uploadedCount, int requiredCount,
                              Set<PhotoType> missingTypes, boolean complete) {
        this.draftId       = draftId;
        this.barcode       = barcode;
        this.status        = status;
        this.photos        = photos;
        this.uploadedCount = uploadedCount;
        this.requiredCount = requiredCount;
        this.missingTypes  = missingTypes;
        this.complete      = complete;
    }

    public UUID           getDraftId()       { return draftId; }
    public String         getBarcode()       { return barcode; }
    public String         getStatus()        { return status; }
    public List<Photo>    getPhotos()        { return photos; }
    public int            getUploadedCount() { return uploadedCount; }
    public int            getRequiredCount() { return requiredCount; }
    public Set<PhotoType> getMissingTypes()  { return missingTypes; }
    public boolean        isComplete()       { return complete; }
}
