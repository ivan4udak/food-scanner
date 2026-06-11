package com.foodscanner.domain.policy;

import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.ocr.OcrStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Слой: domain (unit). Правила выбора типа задачи извлечения. */
class ExtractionEligibilityPolicyTest {

    private final ExtractionEligibilityPolicy policy = new ExtractionEligibilityPolicy(100, 0.35);

    @Test
    void textExtractionWhenEnoughTextAndConfidence() {
        assertThat(policy.decide(OcrStatus.NEEDS_REVIEW, 150, 0.6))
            .contains(ExtractionType.TEXT_EXTRACTION);
    }

    @Test
    void imageFallbackWhenShortText() {
        assertThat(policy.decide(OcrStatus.NEEDS_REVIEW, 50, 0.9))
            .contains(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }

    @Test
    void imageFallbackWhenLowConfidence() {
        assertThat(policy.decide(OcrStatus.NEEDS_REVIEW, 500, 0.2))
            .contains(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }

    @Test
    void imageFallbackWhenUnreadable() {
        assertThat(policy.decide(OcrStatus.PHOTO_UNREADABLE, 0, null))
            .contains(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }

    @Test
    void imageFallbackWhenError() {
        assertThat(policy.decide(OcrStatus.ERROR, 0, null))
            .contains(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }

    @Test
    void skipForNonTerminalStatuses() {
        assertThat(policy.decide(OcrStatus.QUEUED, 200, 0.9)).isEmpty();
        assertThat(policy.decide(OcrStatus.IN_PROGRESS_READABLE, 200, 0.9)).isEmpty();
        assertThat(policy.decide(OcrStatus.SUCCESS, 200, 0.9)).isEmpty();
    }
}
