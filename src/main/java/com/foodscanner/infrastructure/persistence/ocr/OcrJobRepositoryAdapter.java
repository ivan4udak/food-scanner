package com.foodscanner.infrastructure.persistence.ocr;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Слой: infrastructure. Адаптер OcrJobRepository → JPA. */
@Repository
public class OcrJobRepositoryAdapter implements OcrJobRepository {

    private final OcrJobJpaRepository jpa;

    public OcrJobRepositoryAdapter(OcrJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public OcrJob save(OcrJob job) {
        if (jpa.existsById(job.id())) {
            // обновляем только результат-поля — lifecycle (active/orphaned/publish*) не трогаем
            jpa.updateResult(job.id(), (short) job.status().code(), job.attempts(), job.rawText(),
                job.parsedIngredients(), job.parsedNutrition(), job.confidence(),
                job.errorCode(), job.errorMessage(), job.updatedAt());
        } else {
            jpa.save(toJpa(job));
        }
        return job;
    }

    @Override
    @Transactional
    public int supersedePrevious(UUID draftId, String photoType, UUID newJobId) {
        if (draftId == null) return 0;
        return jpa.supersedePrevious(draftId, photoType, newJobId, Instant.now());
    }

    @Override
    @Transactional
    public int markOrphans() {
        return jpa.markOrphans(Instant.now());
    }

    @Override
    @Transactional
    public void markPublished(UUID id) {
        jpa.markPublished(id, Instant.now());
    }

    @Override
    @Transactional
    public void recordPublishFailure(UUID id, String error) {
        jpa.recordPublishFailure(id, error == null ? "publish failed" : error);
    }

    @Override
    public List<OcrJob> findRepublishable() {
        return jpa.findTop50ByActiveTrueAndStatusAndPublishedAtIsNullOrderByCreatedAtAsc(
            (short) OcrStatus.QUEUED.code()).stream().map(OcrJobRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countActiveQueued() {
        return jpa.countByActiveTrueAndStatus((short) OcrStatus.QUEUED.code());
    }

    @Override
    public Optional<Instant> oldestQueuedCreatedAt() {
        return jpa.findTopByActiveTrueAndStatusOrderByCreatedAtAsc((short) OcrStatus.QUEUED.code())
            .map(OcrJobJpaEntity::getCreatedAt);
    }

    @Override
    public java.util.Optional<OcrJob> findById(UUID id) {
        return jpa.findById(id).map(OcrJobRepositoryAdapter::toDomain);
    }

    @Override
    public List<OcrJob> findByDraftId(UUID draftId) {
        return jpa.findByDraftId(draftId).stream().map(OcrJobRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Map<OcrStatus, Long> countByStatus() {
        Map<OcrStatus, Long> counts = new EnumMap<>(OcrStatus.class);
        for (OcrStatus s : OcrStatus.values()) counts.put(s, 0L);
        for (OcrJobJpaRepository.StatusCount row : jpa.countGroupedByStatus()) {
            counts.put(OcrStatus.fromCode(row.getStatus()), row.getCnt());
        }
        return counts;
    }

    private static OcrJobJpaEntity toJpa(OcrJob j) {
        return new OcrJobJpaEntity(
            j.id(), j.draftId(), j.catalogEntryId(), j.storageKey(), j.photoType(),
            (short) j.status().code(), j.attempts(), j.rawText(), j.parsedIngredients(),
            j.parsedNutrition(), j.confidence(), j.errorCode(), j.errorMessage(),
            j.createdAt(), j.updatedAt());
    }

    private static OcrJob toDomain(OcrJobJpaEntity e) {
        return new OcrJob(
            e.getId(), e.getDraftId(), e.getCatalogEntryId(), e.getStorageKey(), e.getPhotoType(),
            OcrStatus.fromCode(e.getStatus()), e.getAttempts(), e.getRawText(), e.getParsedIngredients(),
            e.getParsedNutrition(), e.getConfidence(), e.getErrorCode(), e.getErrorMessage(),
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
