package com.foodscanner.infrastructure.persistence.draft;

import com.foodscanner.domain.model.*;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CatalogDraftRepositoryAdapter implements CatalogDraftRepository {

    private final CatalogDraftJpaRepository jpa;

    public CatalogDraftRepositoryAdapter(CatalogDraftJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CatalogDraft save(CatalogDraft draft) {
        CatalogDraftJpaEntity entity = toJpa(draft);
        jpa.save(entity);
        return draft;
    }

    @Override
    public Optional<CatalogDraft> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CatalogDraft> findOpenByBarcodeAndContributor(
            Barcode barcode, UUID contributorId) {
        return jpa.findByBarcodeAndContributorIdAndStatus(
                barcode.getValue(), contributorId, CatalogDraftStatus.OPEN.name())
            .map(this::toDomain);
    }

    // ──────────────────────────────────────────────
    private CatalogDraftJpaEntity toJpa(CatalogDraft d) {
        CatalogDraftJpaEntity entity = new CatalogDraftJpaEntity(
            d.getId(), d.getBarcode().getValue(), d.getContributorId(),
            d.getStatus().name(), d.getCreatedAt(), d.getUpdatedAt());

        List<DraftPhotoJpaEntity> photoEntities = d.getPhotos().stream()
            .map(p -> new DraftPhotoJpaEntity(
                p.getId(), p.getDraftId(), p.getType().name(),
                p.getStorageKey(), p.getCreatedAt(), p.getCreatedAt(), p.getCapturedAt()))
            .collect(Collectors.toList());

        entity.setPhotos(photoEntities);
        return entity;
    }

    private CatalogDraft toDomain(CatalogDraftJpaEntity e) {
        List<DraftPhoto> photos = e.getPhotos().stream()
            .map(p -> new DraftPhoto(
                p.getId(), p.getDraftId(),
                PhotoType.valueOf(p.getType()),
                p.getStorageKey(), p.getCreatedAt(), p.getCapturedAt()))
            .collect(Collectors.toList());

        return CatalogDraft.reconstitute(
            e.getId(), new Barcode(e.getBarcode()), e.getContributorId(),
            CatalogDraftStatus.valueOf(e.getStatus()),
            photos, e.getCreatedAt(), e.getUpdatedAt());
    }
}
