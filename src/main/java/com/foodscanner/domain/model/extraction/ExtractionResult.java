package com.foodscanner.domain.model.extraction;

/**
 * Слой: domain (Value Object). Результат работы ProductExtractor.
 * nutritionJson/confidenceJson — сериализованный JSON (адаптер пишет в jsonb). source — TEXT/IMAGE/STUB.
 */
public record ExtractionResult(
        String name,
        String brand,
        String manufacturer,
        String composition,
        String nutritionJson,
        String confidenceJson,
        String source,
        boolean needsReview
) {
    /** Пусто — ничего не извлечено (например, stub). */
    public static ExtractionResult empty(String source) {
        return new ExtractionResult(null, null, null, null, null, null, source, false);
    }

    /** Извлечено ли хоть одно структурное поле. */
    public boolean hasAny() {
        return name != null || brand != null || manufacturer != null
            || composition != null || nutritionJson != null;
    }
}
