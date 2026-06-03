package com.foodscanner.api.dto;

import java.util.Set;

public class AddDraftPhotoResponse {
    private int         uploadedCount;
    private int         requiredCount;
    private Set<String> missingTypes;
    private boolean     complete;

    public AddDraftPhotoResponse() {}
    public AddDraftPhotoResponse(int uploadedCount, int requiredCount,
                                 Set<String> missingTypes, boolean complete) {
        this.uploadedCount = uploadedCount;
        this.requiredCount = requiredCount;
        this.missingTypes  = missingTypes;
        this.complete      = complete;
    }

    public int         getUploadedCount()              { return uploadedCount; }
    public int         getRequiredCount()              { return requiredCount; }
    public Set<String> getMissingTypes()               { return missingTypes; }
    public boolean     isComplete()                    { return complete; }
    public void        setUploadedCount(int v)         { this.uploadedCount = v; }
    public void        setRequiredCount(int v)         { this.requiredCount = v; }
    public void        setMissingTypes(Set<String> v)  { this.missingTypes = v; }
    public void        setComplete(boolean v)          { this.complete = v; }
}
