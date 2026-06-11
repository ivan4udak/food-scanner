package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import com.foodscanner.infrastructure.persistence.extraction.ProductExtractionJobJpaEntity;
import com.foodscanner.infrastructure.persistence.extraction.ProductExtractionJobJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Слой: infrastructure (IT). Сохранение задачи извлечения против Postgres. */
class ProductExtractionJobRepositoryAdapterIT extends AbstractRepositoryIT {

    @Autowired ProductExtractionJobRepository repo;
    @Autowired ProductExtractionJobJpaRepository jpa;

    @Test
    void savesQueuedJob() {
        UUID ocrJobId = UUID.randomUUID();
        ProductExtractionJob job = repo.save(
            ProductExtractionJob.queued(ocrJobId, "4680328054884", ExtractionType.IMAGE_FALLBACK_EXTRACTION));

        ProductExtractionJobJpaEntity row = jpa.findById(job.id()).orElseThrow();
        assertThat(row.getOcrJobId()).isEqualTo(ocrJobId);
        assertThat(row.getBarcode()).isEqualTo("4680328054884");
        assertThat(row.getType()).isEqualTo("IMAGE_FALLBACK_EXTRACTION");
        assertThat(row.getStatus()).isZero();   // QUEUED
        assertThat(row.getAttempts()).isZero();
    }

    @Test
    void workerLifecycleFindQueuedInProgressApplyResult() {
        UUID ocrJobId = UUID.randomUUID();
        ProductExtractionJob job = repo.save(
            ProductExtractionJob.queued(ocrJobId, "461", ExtractionType.TEXT_EXTRACTION));

        assertThat(repo.findQueued(10)).anyMatch(j -> j.id().equals(job.id()));

        repo.markInProgress(job.id());
        assertThat(jpa.findById(job.id()).orElseThrow().getStatus())
            .isEqualTo((short) ExtractionStatus.IN_PROGRESS.code());

        repo.applyResult(job.id(), ExtractionStatus.STRUCTURED,
            new ExtractionResult("Печенье", "BrandX", null, "вода;сахар", null, null, "TEXT", false));
        ProductExtractionJobJpaEntity done = jpa.findById(job.id()).orElseThrow();
        assertThat(done.getStatus()).isEqualTo((short) ExtractionStatus.STRUCTURED.code());

        // после применения результата задача больше не QUEUED → не в выборке
        assertThat(repo.findQueued(10)).noneMatch(j -> j.id().equals(job.id()));
    }

    @Test
    void metricsCountByStatusQueueAndOldest() {
        long queuedBefore = repo.countQueued();
        ProductExtractionJob q1 = repo.save(
            ProductExtractionJob.queued(UUID.randomUUID(), "462", ExtractionType.TEXT_EXTRACTION));
        repo.save(ProductExtractionJob.queued(UUID.randomUUID(), "463", ExtractionType.TEXT_EXTRACTION));

        assertThat(repo.countQueued()).isEqualTo(queuedBefore + 2);
        assertThat(repo.countByStatus().getOrDefault(ExtractionStatus.QUEUED, 0L))
            .isGreaterThanOrEqualTo(2L);
        assertThat(repo.oldestQueuedAt()).isPresent();

        // перевод одной в SKIPPED уменьшает очередь и добавляет SKIPPED в разбивку
        repo.skip(q1.id(), "test");
        assertThat(repo.countQueued()).isEqualTo(queuedBefore + 1);
        assertThat(repo.countByStatus().getOrDefault(ExtractionStatus.SKIPPED, 0L))
            .isGreaterThanOrEqualTo(1L);
    }
}
