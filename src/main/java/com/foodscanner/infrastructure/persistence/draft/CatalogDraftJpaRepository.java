package com.foodscanner.infrastructure.persistence.draft;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogDraftJpaRepository
        extends JpaRepository<CatalogDraftJpaEntity, UUID> {

    Optional<CatalogDraftJpaEntity> findByBarcodeAndContributorIdAndStatus(
        String barcode, UUID contributorId, String status);

    @Query("select d from CatalogDraftJpaEntity d " +
           "where d.status in ('OPEN','ABANDONED') and d.createdAt < :cutoff")
    List<CatalogDraftJpaEntity> findStaleUnfinished(Instant cutoff);
}
