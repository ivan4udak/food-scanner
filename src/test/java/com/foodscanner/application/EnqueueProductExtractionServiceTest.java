package com.foodscanner.application;

import com.foodscanner.application.service.EnqueueProductExtractionService;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Слой: application (unit). Создание задачи извлечения по политике + флаг enabled. */
class EnqueueProductExtractionServiceTest {

    private final ProductExtractionJobRepository repo = mock(ProductExtractionJobRepository.class);
    private final EnqueueProductExtractionService service =
        new EnqueueProductExtractionService(repo, true, 100, 0.35);

    @Test
    void createsTextExtractionForGoodText() {
        UUID ocr = UUID.randomUUID();
        service.onOcrResult(ocr, OcrStatus.NEEDS_REVIEW, "x".repeat(200), 0.7, "460");

        ArgumentCaptor<ProductExtractionJob> cap = ArgumentCaptor.forClass(ProductExtractionJob.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().type()).isEqualTo(ExtractionType.TEXT_EXTRACTION);
        assertThat(cap.getValue().status()).isEqualTo(ExtractionStatus.QUEUED);
        assertThat(cap.getValue().ocrJobId()).isEqualTo(ocr);
    }

    @Test
    void createsImageFallbackForUnreadable() {
        service.onOcrResult(UUID.randomUUID(), OcrStatus.PHOTO_UNREADABLE, "", null, null);
        ArgumentCaptor<ProductExtractionJob> cap = ArgumentCaptor.forClass(ProductExtractionJob.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().type()).isEqualTo(ExtractionType.IMAGE_FALLBACK_EXTRACTION);
    }

    @Test
    void skipsNonTerminalStatus() {
        service.onOcrResult(UUID.randomUUID(), OcrStatus.SUCCESS, "x".repeat(200), 0.9, null);
        verify(repo, never()).save(any());
    }

    @Test
    void respectsDisabledFlag() {
        EnqueueProductExtractionService disabled =
            new EnqueueProductExtractionService(repo, false, 100, 0.35);
        disabled.onOcrResult(UUID.randomUUID(), OcrStatus.NEEDS_REVIEW, "x".repeat(200), 0.9, null);
        verify(repo, never()).save(any());
    }
}
