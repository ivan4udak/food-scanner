package com.foodscanner.domain.model.extraction;

/**
 * Слой: domain.
 * Статус задачи структурного извлечения продукта (отдельно от OCR-статусов).
 * 0 QUEUED · 1 IN_PROGRESS · 2 STRUCTURED · 3 NEEDS_REVIEW · 4 FAILED · 5 SKIPPED.
 */
public enum ExtractionStatus {
    QUEUED(0),
    IN_PROGRESS(1),
    STRUCTURED(2),
    NEEDS_REVIEW(3),
    FAILED(4),
    SKIPPED(5);

    private final int code;

    ExtractionStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ExtractionStatus fromCode(int code) {
        for (ExtractionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown extraction status code: " + code);
    }
}
