package com.foodscanner.application.result;

import java.util.UUID;

/**
 * Слой: application
 * Результат ScanBarcodeUseCase.
 *
 * status NEW  → draftId присутствует, UI показывает зелёный экран
 * status EXISTS → draftId null, UI показывает красный экран
 */
public final class ScanBarcodeResult {

    public enum ScanStatus { NEW, EXISTS }

    private final ScanStatus status;
    private final UUID       draftId;  // null если EXISTS

    private ScanBarcodeResult(ScanStatus status, UUID draftId) {
        this.status  = status;
        this.draftId = draftId;
    }

    public static ScanBarcodeResult newProduct(UUID draftId) {
        return new ScanBarcodeResult(ScanStatus.NEW, draftId);
    }

    public static ScanBarcodeResult alreadyExists() {
        return new ScanBarcodeResult(ScanStatus.EXISTS, null);
    }

    public ScanStatus getStatus()  { return status; }
    public UUID       getDraftId() { return draftId; }
}
