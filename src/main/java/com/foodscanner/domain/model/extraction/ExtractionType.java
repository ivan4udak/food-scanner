package com.foodscanner.domain.model.extraction;

/**
 * Слой: domain.
 * Тип извлечения: по сырому тексту OCR либо по исходному фото (fallback, когда OCR пуст/плох).
 */
public enum ExtractionType {
    TEXT_EXTRACTION,
    IMAGE_FALLBACK_EXTRACTION
}
