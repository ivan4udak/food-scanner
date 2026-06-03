package com.foodscanner.api.dto;

import java.util.UUID;

/**
 * status: "NEW"    → зелёный экран, draftId присутствует
 * status: "EXISTS" → красный экран, draftId = null
 */
public class ScanBarcodeResponse {
    private String status;
    private UUID   draftId;

    public ScanBarcodeResponse() {}
    public ScanBarcodeResponse(String status, UUID draftId) {
        this.status  = status;
        this.draftId = draftId;
    }

    public String getStatus()             { return status; }
    public UUID   getDraftId()            { return draftId; }
    public void   setStatus(String v)     { this.status = v; }
    public void   setDraftId(UUID v)      { this.draftId = v; }
}
