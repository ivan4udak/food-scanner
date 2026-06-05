package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class ScanBarcodeRequest {

    @NotBlank(message = "Barcode must not be blank")
    private String barcodeValue;

    // Пользователь берётся из JWT (см. CatalogController.scan), поэтому поле опционально:
    // клиент может его не присылать. Оставлено для обратной совместимости (iOS шлёт).
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
