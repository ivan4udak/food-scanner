package com.foodscanner.application.command;

import java.util.UUID;

public final class ScanBarcodeCommand {
    private final String barcodeValue;
    private final UUID   contributorId;

    public ScanBarcodeCommand(String barcodeValue, UUID contributorId) {
        this.barcodeValue  = barcodeValue;
        this.contributorId = contributorId;
    }

    public String getBarcodeValue()  { return barcodeValue; }
    public UUID   getContributorId() { return contributorId; }
}
