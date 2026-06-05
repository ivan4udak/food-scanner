package com.foodscanner.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Ответ GET /api/v1/drafts/{id} — состояние черновика для восстановления на клиенте. */
public class DraftResponse {

    private String        draftId;
    private String        barcode;
    private String        status;        // OPEN | COMPLETED | ABANDONED
    private List<PhotoDto> photos;        // по одному (последнему) фото на тип
    private int           uploadedCount; // сколько обязательных типов загружено
    private int           requiredCount; // = 4
    private Set<String>   missingTypes;
    private boolean       complete;

    public static class PhotoDto {
        private String  type;
        private String  storageKey;
        private Instant capturedAt;

        public PhotoDto() {}
        public PhotoDto(String type, String storageKey, Instant capturedAt) {
            this.type = type;
            this.storageKey = storageKey;
            this.capturedAt = capturedAt;
        }
        public String  getType()       { return type; }
        public String  getStorageKey() { return storageKey; }
        public Instant getCapturedAt() { return capturedAt; }
        public void setType(String v)       { this.type = v; }
        public void setStorageKey(String v) { this.storageKey = v; }
        public void setCapturedAt(Instant v){ this.capturedAt = v; }
    }

    public DraftResponse() {}
    public DraftResponse(String draftId, String barcode, String status, List<PhotoDto> photos,
                         int uploadedCount, int requiredCount, Set<String> missingTypes, boolean complete) {
        this.draftId       = draftId;
        this.barcode       = barcode;
        this.status        = status;
        this.photos        = photos;
        this.uploadedCount = uploadedCount;
        this.requiredCount = requiredCount;
        this.missingTypes  = missingTypes;
        this.complete      = complete;
    }

    public String        getDraftId()       { return draftId; }
    public String        getBarcode()       { return barcode; }
    public String        getStatus()        { return status; }
    public List<PhotoDto> getPhotos()       { return photos; }
    public int           getUploadedCount() { return uploadedCount; }
    public int           getRequiredCount() { return requiredCount; }
    public Set<String>   getMissingTypes()  { return missingTypes; }
    public boolean       isComplete()       { return complete; }
}
