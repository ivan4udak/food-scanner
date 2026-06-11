package com.foodscanner.infrastructure.persistence.extraction;

import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.springframework.stereotype.Repository;

/** Слой: infrastructure. Адаптер ProductExtractionJobRepository → JPA. */
@Repository
public class ProductExtractionJobRepositoryAdapter implements ProductExtractionJobRepository {

    private final ProductExtractionJobJpaRepository jpa;

    public ProductExtractionJobRepositoryAdapter(ProductExtractionJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ProductExtractionJob save(ProductExtractionJob job) {
        jpa.save(new ProductExtractionJobJpaEntity(
            job.id(), job.ocrJobId(), job.barcode(), job.type().name(), (short) job.status().code(),
            job.attempts(), job.queuedAt(), job.createdAt(), job.updatedAt()));
        return job;
    }
}
