package com.foodscanner.application;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.application.service.ReprocessOcrService;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Слой: application (unit). Reprocess только для статусов 3/5; создаёт+замещает+публикует. */
class ReprocessOcrServiceTest {

    private final OcrJobRepository repo = mock(OcrJobRepository.class);
    private final OcrJobPublisher publisher = mock(OcrJobPublisher.class);
    private final ReprocessOcrService service = new ReprocessOcrService(repo, publisher);

    private OcrJob job(UUID id, OcrStatus status) {
        Instant now = Instant.now();
        return new OcrJob(id, UUID.randomUUID(), null, "photos/x.jpg", "INGREDIENTS",
            status, 1, null, null, null, null, null, null, now, now);
    }

    @Test
    void reprocessesUnreadable() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(job(id, OcrStatus.PHOTO_UNREADABLE)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(publisher.publish(any())).thenReturn(true);

        Optional<UUID> result = service.execute(id);

        assertThat(result).isPresent();
        verify(repo).save(argThat(j -> j.status() == OcrStatus.QUEUED && j.photoType().equals("INGREDIENTS")));
        verify(repo).supersede(eq(id), any());
        verify(repo).markPublished(any());
    }

    @Test
    void rejectsNonReprocessableStatus() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(job(id, OcrStatus.NEEDS_REVIEW)));
        assertThatThrownBy(() -> service.execute(id)).isInstanceOf(IllegalStateException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void emptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThat(service.execute(id)).isEmpty();
        verify(repo, never()).save(any());
    }
}
