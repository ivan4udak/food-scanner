package com.foodscanner.application;

import com.foodscanner.application.service.EnqueueOcrService;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EnqueueOcrServiceTest {

    private final OcrJobRepository repo = mock(OcrJobRepository.class);
    private final EnqueueOcrService service = new EnqueueOcrService(repo);

    @Test
    void enqueuesForTextPhoto() {
        UUID draft = UUID.randomUUID();
        service.execute(draft, "photos/h.jpg", "INGREDIENTS");

        ArgumentCaptor<OcrJob> cap = ArgumentCaptor.forClass(OcrJob.class);
        verify(repo).save(cap.capture());
        OcrJob j = cap.getValue();
        assertThat(j.status()).isEqualTo(OcrStatus.QUEUED);
        assertThat(j.photoType()).isEqualTo("INGREDIENTS");
        assertThat(j.draftId()).isEqualTo(draft);
    }

    @Test
    void skipsNonTextPhotos() {
        service.execute(UUID.randomUUID(), "photos/h.jpg", "FRONT");
        service.execute(UUID.randomUUID(), "photos/h.jpg", "BARCODE");
        verify(repo, never()).save(any());
    }

    @Test
    void caseInsensitive() {
        service.execute(UUID.randomUUID(), "k", "nutrition");
        verify(repo).save(any());
    }
}
