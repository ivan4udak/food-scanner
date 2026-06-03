package com.foodscanner.infrastructure.persistence.draft;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CatalogDraftJpaRepository
        extends JpaRepository<CatalogDraftJpaEntity, UUID> {

    Optional<CatalogDraftJpaEntity> findByBarcodeAndContributorIdAndStatus(
        String barcode, UUID contributorId, String status);
}
