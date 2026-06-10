package com.foodscanner.application;

import com.foodscanner.application.service.UpdateOcrResultService;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UpdateOcrResultServiceTest {

    private final OcrJobRepository repo = mock(OcrJobRepository.class);
    private final UpdateOcrResultService service = new UpdateOcrResultService(repo);

    @Test
    void appliesResultPreservingIdentity() {
        OcrJob job = OcrJob.queued(UUID.randomUUID(), "photos/h.jpg", "NUTRITION");
        when(repo.findById(job.id())).thenReturn(Optional.of(job));

        service.execute(job.id(), OcrStatus.SUCCESS, "raw", "состав", "{\"kcal\":250}", 0.9, null, null);

        ArgumentCaptor<OcrJob> cap = ArgumentCaptor.forClass(OcrJob.class);
        verify(repo).save(cap.capture());
        OcrJob saved = cap.getValue();
        assertThat(saved.id()).isEqualTo(job.id());
        assertThat(saved.storageKey()).isEqualTo("photos/h.jpg");
        assertThat(saved.status()).isEqualTo(OcrStatus.SUCCESS);
        assertThat(saved.rawText()).isEqualTo("raw");
        assertThat(saved.attempts()).isEqualTo(1);
    }

    @Test
    void ignoresUnknownJob() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        service.execute(id, OcrStatus.SUCCESS, null, null, null, null, null, null);
        verify(repo, never()).save(any());
    }
}
