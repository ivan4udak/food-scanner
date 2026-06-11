package com.foodscanner.infrastructure.scheduling;

import com.foodscanner.application.port.ProductExtractor;
import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.repository.OcrJobRepository;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Слой: infrastructure (unit). Воркер: обработка батча, статусы, FAILED, флаг enabled. */
class ProductExtractionWorkerTest {

    private final ProductExtractionJobRepository repo = mock(ProductExtractionJobRepository.class);
    private final ProductExtractor extractor = mock(ProductExtractor.class);
    private final OcrJobRepository ocrJobs = mock(OcrJobRepository.class);

    // окно 00:00–00:00 = 24/7, чтобы run() всегда внутри окна в тесте
    private ProductExtractionWorker worker(boolean enabled) {
        return new ProductExtractionWorker(repo, extractor, ocrJobs, enabled,
            "00:00", "00:00", "UTC", 50, 240);
    }

    private ProductExtractionJob queuedJob() {
        return ProductExtractionJob.queued(UUID.randomUUID(), "460", ExtractionType.TEXT_EXTRACTION);
    }

    @Test
    void stubResultMarksSkipped() {
        ProductExtractionJob job = queuedJob();
        when(repo.findQueued(anyInt())).thenReturn(List.of(job));
        when(ocrJobs.findById(any())).thenReturn(Optional.of(
            OcrJob.queued(UUID.randomUUID(), "p.jpg", "INGREDIENTS")));
        when(extractor.extract(eq(job), any())).thenReturn(ExtractionResult.empty("STUB"));

        worker(true).run();

        verify(repo).markInProgress(job.id());
        verify(repo).applyResult(eq(job.id()), eq(ExtractionStatus.SKIPPED), any());
        verify(repo, never()).markFailed(any(), any());
    }

    @Test
    void structuredResultMarksStructured() {
        ProductExtractionJob job = queuedJob();
        when(repo.findQueued(anyInt())).thenReturn(List.of(job));
        when(ocrJobs.findById(any())).thenReturn(Optional.empty());
        when(extractor.extract(eq(job), any())).thenReturn(
            new ExtractionResult("Печенье", null, null, null, null, null, "TEXT", false));

        worker(true).run();

        verify(repo).applyResult(eq(job.id()), eq(ExtractionStatus.STRUCTURED), any());
    }

    @Test
    void exceptionMarksFailed() {
        ProductExtractionJob job = queuedJob();
        when(repo.findQueued(anyInt())).thenReturn(List.of(job));
        when(ocrJobs.findById(any())).thenReturn(Optional.empty());
        when(extractor.extract(eq(job), any())).thenThrow(new RuntimeException("boom"));

        worker(true).run();

        verify(repo).markFailed(eq(job.id()), eq("boom"));
    }

    @Test
    void disabledDoesNothing() {
        worker(false).run();
        verifyNoInteractions(repo, extractor, ocrJobs);
    }
}
