package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ScanBarcodeRequest {

    @NotBlank(message = "Barcode must not be blank")
    private String barcodeValue;

    @NotNull(message = "ContributorId must not be null")
    private UUID contributorId;

    public ScanBarcodeRequest() {}
    public ScanBarcodeRequest(String barcodeValue, UUID contributorId) {
        this.barcodeValue  = barcodeValue;
        this.contributorId = contributorId;
    }

    public String getBarcodeValue()             { return barcodeValue; }
    public UUID   getContributorId()            { return contributorId; }
    public void   setBarcodeValue(String v)     { this.barcodeValue = v; }
    public void   setContributorId(UUID v)      { this.contributorId = v; }
}
