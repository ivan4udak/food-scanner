package com.foodscanner.domain.model.ocr;

/**
 * Слой: domain.
 * Статус OCR-задачи (коды 0–5, см. docs/OCR.md).
 */
public enum OcrStatus {
    QUEUED(0),
    IN_PROGRESS_READABLE(1),
    NEEDS_REVIEW(2),
    PHOTO_UNREADABLE(3),
    SUCCESS(4),
    ERROR(5);

    private final int code;

    OcrStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static OcrStatus fromCode(int code) {
        for (OcrStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown OCR status code: " + code);
    }
}
