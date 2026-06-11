package com.foodscanner.domain.policy;

import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.ocr.OcrStatus;

import java.util.Optional;

/**
 * Слой: domain (policy, чистая логика).
 * Решает, нужна ли задача извлечения и какого типа — по результату OCR.
 *
 * Правила (пороги задаются снаружи, не хардкод):
 *   - OCR PHOTO_UNREADABLE(3) → IMAGE_FALLBACK (текста нет);
 *   - достаточно текста (len ≥ minLen) и уверенность ≥ minConf → TEXT_EXTRACTION;
 *   - текста мало / низкая уверенность → IMAGE_FALLBACK (пробуем по фото);
 *   - нетерминальные/уже-структурированные OCR-статусы → пропуск (Optional.empty).
 * Рассматриваем только терминальные «с фото» OCR-статусы: NEEDS_REVIEW(2), PHOTO_UNREADABLE(3), ERROR(5).
 */
public class ExtractionEligibilityPolicy {

    private final int minRawTextLength;
    private final double minConfidence;

    public ExtractionEligibilityPolicy(int minRawTextLength, double minConfidence) {
        this.minRawTextLength = minRawTextLength;
        this.minConfidence = minConfidence;
    }

    /** @return тип задачи извлечения, либо empty если для этого OCR-результата извлечение не нужно. */
    public Optional<ExtractionType> decide(OcrStatus ocrStatus, int rawTextLength, Double confidence) {
        int code = ocrStatus.code();
        boolean terminalWithPhoto = code == OcrStatus.NEEDS_REVIEW.code()
            || code == OcrStatus.PHOTO_UNREADABLE.code()
            || code == OcrStatus.ERROR.code();
        if (!terminalWithPhoto) return Optional.empty();

        if (code == OcrStatus.PHOTO_UNREADABLE.code() || code == OcrStatus.ERROR.code()) {
            return Optional.of(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
        }
        double conf = confidence == null ? 0.0 : confidence;
        if (rawTextLength >= minRawTextLength && conf >= minConfidence) {
            return Optional.of(ExtractionType.TEXT_EXTRACTION);
        }
        return Optional.of(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }
}
