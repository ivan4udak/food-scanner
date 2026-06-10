package com.foodscanner.infrastructure.persistence.ocr;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Слой: infrastructure. Адаптер OcrJobRepository → JPA. */
@Repository
public class OcrJobRepositoryAdapter implements OcrJobRepository {

    private final OcrJobJpaRepository jpa;

    public OcrJobRepositoryAdapter(OcrJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OcrJob save(OcrJob job) {
        jpa.save(toJpa(job));
        return job;
    }

    @Override
    public java.util.Optional<OcrJob> findById(UUID id) {
        return jpa.findById(id).map(OcrJobRepositoryAdapter::toDomain);
    }

    @Override
    public List<OcrJob> findByDraftId(UUID draftId) {
        return jpa.findByDraftId(draftId).stream().map(OcrJobRepositoryAdapter::toDomain).toList();
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
