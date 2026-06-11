package com.foodscanner.application;

import com.foodscanner.application.service.RequeueExtractionService;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Слой: application (unit). Requeue: новая QUEUED для разрешённых статусов. */
class RequeueExtractionServiceTest {

    private final ProductExtractionJobRepository repo = mock(ProductExtractionJobRepository.class);
    private final RequeueExtractionService service = new RequeueExtractionService(repo);

    private ProductExtractionJob job(ExtractionStatus status) {
        Instant now = Instant.now();
        return new ProductExtractionJob(UUID.randomUUID(), UUID.randomUUID(), "460",
            ExtractionType.TEXT_EXTRACTION, status, 1, now, now, now);
    }

    @Test
    void allowsNeedsReviewFailedSkipped() {
        for (ExtractionStatus s : new ExtractionStatus[]{
                ExtractionStatus.NEEDS_REVIEW, ExtractionStatus.FAILED, ExtractionStatus.SKIPPED}) {
            ProductExtractionJob old = job(s);
            when(repo.findById(old.id())).thenReturn(Optional.of(old));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<ProductExtractionJob> cap = ArgumentCaptor.forClass(ProductExtractionJob.class);
            Optional<UUID> newId = service.execute(old.id());

            verify(repo, atLeastOnce()).save(cap.capture());
            ProductExtractionJob fresh = cap.getValue();
            assertThat(fresh.status()).isEqualTo(ExtractionStatus.QUEUED);
            assertThat(fresh.attempts()).isZero();
            assertThat(fresh.ocrJobId()).isEqualTo(old.ocrJobId());
            assertThat(fresh.barcode()).isEqualTo(old.barcode());
            assertThat(fresh.type()).isEqualTo(old.type());
            assertThat(fresh.id()).isNotEqualTo(old.id());
            assertThat(newId).contains(fresh.id());
            reset(repo);
        }
    }

    @Test
    void rejectsQueuedInProgressStructured() {
        for (ExtractionStatus s : new ExtractionStatus[]{
                ExtractionStatus.QUEUED, ExtractionStatus.IN_PROGRESS, ExtractionStatus.STRUCTURED}) {
            ProductExtractionJob old = job(s);
            when(repo.findById(old.id())).thenReturn(Optional.of(old));
            assertThatThrownBy(() -> service.execute(old.id()))
                .isInstanceOf(IllegalStateException.class);
            verify(repo, never()).save(any());
            reset(repo);
        }
    }

    @Test
    void emptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThat(service.execute(id)).isEmpty();
        verify(repo, never()).save(any());
    }
}
