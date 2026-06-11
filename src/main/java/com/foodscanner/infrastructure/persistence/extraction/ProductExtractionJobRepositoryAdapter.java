package com.foodscanner.infrastructure.persistence.extraction;

import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Слой: infrastructure. Адаптер ProductExtractionJobRepository → JPA. */
@Repository
public class ProductExtractionJobRepositoryAdapter implements ProductExtractionJobRepository {

    private final ProductExtractionJobJpaRepository jpa;

    public ProductExtractionJobRepositoryAdapter(ProductExtractionJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public ProductExtractionJob save(ProductExtractionJob job) {
        jpa.save(new ProductExtractionJobJpaEntity(
            job.id(), job.ocrJobId(), job.barcode(), job.type().name(), (short) job.status().code(),
            job.attempts(), job.queuedAt(), job.createdAt(), job.updatedAt()));
        return job;
    }

    @Override
    public Optional<ProductExtractionJob> findById(UUID id) {
        return jpa.findById(id).map(ProductExtractionJobRepositoryAdapter::toDomain);
    }

    @Override
    public List<ProductExtractionJob> findQueued(int limit) {
        return jpa.findByStatusOrderByQueuedAtAsc((short) ExtractionStatus.QUEUED.code(),
                PageRequest.of(0, Math.max(1, limit)))
            .stream().map(ProductExtractionJobRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public void markInProgress(UUID id) {
        jpa.markInProgress(id, (short) ExtractionStatus.IN_PROGRESS.code(), Instant.now());
    }

    @Override
    @Transactional
    public void applyResult(UUID id, ExtractionStatus status, ExtractionResult r) {
        jpa.applyResult(id, (short) status.code(), r.source(), r.name(), r.brand(), r.manufacturer(),
            r.composition(), r.nutritionJson(), r.confidenceJson(), r.needsReview(), Instant.now());
    }

    @Override
    @Transactional
    public void markFailed(UUID id, String error) {
        jpa.markFailed(id, (short) ExtractionStatus.FAILED.code(), error, Instant.now());
    }

    @Override
    @Transactional
    public void skip(UUID id, String reason) {
        jpa.markSkipped(id, (short) ExtractionStatus.SKIPPED.code(), reason, Instant.now());
    }

    @Override
    public Map<ExtractionStatus, Long> countByStatus() {
        Map<ExtractionStatus, Long> counts = new EnumMap<>(ExtractionStatus.class);
        for (Object[] row : jpa.countGroupedByStatus()) {
            int code = ((Number) row[0]).intValue();
            counts.put(ExtractionStatus.fromCode(code), ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public long countQueued() {
        return jpa.countByStatus((short) ExtractionStatus.QUEUED.code());
    }

    @Override
    public Optional<Instant> oldestQueuedAt() {
        return Optional.ofNullable(jpa.oldestQueuedAt((short) ExtractionStatus.QUEUED.code()));
    }

    private static ProductExtractionJob toDomain(ProductExtractionJobJpaEntity e) {
        return new ProductExtractionJob(
            e.getId(), e.getOcrJobId(), e.getBarcode(), ExtractionType.valueOf(e.getType()),
            ExtractionStatus.fromCode(e.getStatus()), e.getAttempts(),
            e.getQueuedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
