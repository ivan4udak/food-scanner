package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.foodscanner.domain.model.ocr.OcrStatus;

import java.util.List;
import java.util.Map;
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

    @Test
    void countsByStatusWithZeroFill() {
        repo.save(OcrJob.queued(UUID.randomUUID(), "p/" + UUID.randomUUID() + ".jpg", "INGREDIENTS"));
        repo.save(OcrJob.queued(UUID.randomUUID(), "p/" + UUID.randomUUID() + ".jpg", "NUTRITION"));

        Map<OcrStatus, Long> counts = repo.countByStatus();

        assertThat(counts).containsKeys(OcrStatus.values()); // все статусы присутствуют
        assertThat(counts.get(OcrStatus.QUEUED)).isGreaterThanOrEqualTo(2L);
        assertThat(counts.get(OcrStatus.SUCCESS)).isZero(); // zero-fill
    }
}
