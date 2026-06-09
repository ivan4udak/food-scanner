package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Слой: infrastructure (IT). OCR job persist/read против Postgres (jsonb parsed_nutrition). */
class OcrJobRepositoryAdapterIT extends AbstractRepositoryIT {

    @Autowired OcrJobRepository repo;

    @Test
    void savesAndReadsByDraft() {
        UUID draft = UUID.randomUUID();
        repo.save(OcrJob.queued(draft, "photos/" + UUID.randomUUID() + ".jpg", "INGREDIENTS"));

        List<OcrJob> jobs = repo.findByDraftId(draft);
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).status()).isEqualTo(OcrStatus.QUEUED);
        assertThat(jobs.get(0).photoType()).isEqualTo("INGREDIENTS");
    }
}
