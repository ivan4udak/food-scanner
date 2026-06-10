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

    @Test
    void lifecycleSupersedeOrphanPublishRepublish() {
        UUID draft = UUID.randomUUID(); // нет реального черновика → кандидат на orphan
        long base = repo.countActiveQueued();

        OcrJob j1 = repo.save(OcrJob.queued(draft, "p/1.jpg", "INGREDIENTS"));
        OcrJob j2 = repo.save(OcrJob.queued(draft, "p/2.jpg", "INGREDIENTS"));
        assertThat(repo.countActiveQueued()).isEqualTo(base + 2);

        // только последняя (j2) активна для (draft, INGREDIENTS)
        repo.supersedePrevious(draft, "INGREDIENTS", j2.id());
        assertThat(repo.countActiveQueued()).isEqualTo(base + 1);

        // неопубликованные активные QUEUED — кандидаты republish: j2 да, j1 (inactive) нет
        assertThat(repo.findRepublishable()).anyMatch(j -> j.id().equals(j2.id()));
        assertThat(repo.findRepublishable()).noneMatch(j -> j.id().equals(j1.id()));

        // публикация убирает j2 из кандидатов
        repo.markPublished(j2.id());
        assertThat(repo.findRepublishable()).noneMatch(j -> j.id().equals(j2.id()));

        // orphan: черновик не существует → активная j2 без entry становится orphaned/inactive
        int orphaned = repo.markOrphans();
        assertThat(orphaned).isGreaterThanOrEqualTo(1);
        assertThat(repo.countActiveQueued()).isEqualTo(base);
        assertThat(repo.oldestQueuedCreatedAt()).isNotNull(); // не бросает
    }

    @Test
    void supersedeByIdMarksInactive() {
        long base = repo.countActiveQueued();
        OcrJob j = repo.save(OcrJob.queued(UUID.randomUUID(), "p/" + UUID.randomUUID() + ".jpg", "NUTRITION"));
        assertThat(repo.countActiveQueued()).isEqualTo(base + 1);

        repo.supersede(j.id(), UUID.randomUUID()); // reprocess замещает старую по id
        assertThat(repo.countActiveQueued()).isEqualTo(base);
    }
}
