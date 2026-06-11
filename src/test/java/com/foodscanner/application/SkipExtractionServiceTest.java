package com.foodscanner.application;

import com.foodscanner.application.service.SkipExtractionService;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Слой: application (unit). Skip: → SKIPPED для разрешённых статусов, иначе IllegalStateException. */
class SkipExtractionServiceTest {

    private final ProductExtractionJobRepository repo = mock(ProductExtractionJobRepository.class);
    private final SkipExtractionService service = new SkipExtractionService(repo);

    private ProductExtractionJob job(ExtractionStatus status) {
        Instant now = Instant.now();
        return new ProductExtractionJob(UUID.randomUUID(), UUID.randomUUID(), "460",
            ExtractionType.TEXT_EXTRACTION, status, 0, now, now, now);
    }

    @Test
    void allowsQueuedNeedsReviewFailed() {
        for (ExtractionStatus s : new ExtractionStatus[]{
                ExtractionStatus.QUEUED, ExtractionStatus.NEEDS_REVIEW, ExtractionStatus.FAILED}) {
            ProductExtractionJob old = job(s);
            when(repo.findById(old.id())).thenReturn(Optional.of(old));

            Optional<UUID> res = service.execute(old.id());

            assertThat(res).contains(old.id());
            verify(repo).skip(eq(old.id()), eq("Skipped by admin"));
            reset(repo);
        }
    }

    @Test
    void rejectsInProgressStructuredSkipped() {
        for (ExtractionStatus s : new ExtractionStatus[]{
                ExtractionStatus.IN_PROGRESS, ExtractionStatus.STRUCTURED, ExtractionStatus.SKIPPED}) {
            ProductExtractionJob old = job(s);
            when(repo.findById(old.id())).thenReturn(Optional.of(old));
            assertThatThrownBy(() -> service.execute(old.id()))
                .isInstanceOf(IllegalStateException.class);
            verify(repo, never()).skip(any(), any());
            reset(repo);
        }
    }

    @Test
    void emptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThat(service.execute(id)).isEmpty();
        verify(repo, never()).skip(any(), any());
    }
}
