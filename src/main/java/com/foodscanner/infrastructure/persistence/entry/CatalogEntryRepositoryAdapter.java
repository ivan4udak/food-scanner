package com.foodscanner.infrastructure.persistence.entry;

import com.foodscanner.domain.model.*;
import com.foodscanner.domain.repository.CatalogEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CatalogEntryRepositoryAdapter implements CatalogEntryRepository {

    private final CatalogEntryJpaRepository jpa;

    public CatalogEntryRepositoryAdapter(CatalogEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CatalogEntry save(CatalogEntry entry) {
        jpa.save(toJpa(entry));
        return entry;
    }

    @Override
    public Optional<CatalogEntry> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CatalogEntry> findByBarcode(Barcode barcode) {
        return jpa.findByBarcode(barcode.getValue()).map(this::toDomain);
    }

    @Override
    public boolean existsByBarcode(Barcode barcode) {
        return jpa.existsByBarcode(barcode.getValue());
    }

    // ──────────────────────────────────────────────
    private CatalogEntryJpaEntity toJpa(CatalogEntry e) {
        CatalogEntryJpaEntity entity = new CatalogEntryJpaEntity(
            e.getId(), e.getBarcode().getValue(), e.getContributorId(),
            e.getDraftId(), e.getCreatedAt(), e.getUpdatedAt());

        List<CatalogEntryPhotoJpaEntity> photos = e.getPhotos().stream()
            .map(p -> new CatalogEntryPhotoJpaEntity(
                p.getId(), p.getEntryId(), p.getType().name(),
                p.getStorageKey(), p.getCreatedAt(), p.getCreatedAt()))
            .collect(Collectors.toList());

        entity.setPhotos(photos);
        return entity;
    }

    private CatalogEntry toDomain(CatalogEntryJpaEntity e) {
        List<CatalogEntryPhoto> photos = e.getPhotos().stream()
            .map(p -> new CatalogEntryPhoto(
                p.getId(), p.getEntryId(),
                PhotoType.valueOf(p.getType()),
                p.getStorageKey(), p.getCreatedAt()))
            .collect(Collectors.toList());

        return CatalogEntry.reconstitute(
            e.getId(), new Barcode(e.getBarcode()), e.getContributorId(),
            e.getDraftId(), photos, e.getCreatedAt(), e.getUpdatedAt());
    }
}
