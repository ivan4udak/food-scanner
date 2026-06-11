package com.foodscanner.infrastructure.persistence;

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
}
