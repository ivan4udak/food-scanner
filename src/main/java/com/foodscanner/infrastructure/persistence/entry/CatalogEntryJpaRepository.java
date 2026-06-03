package com.foodscanner.infrastructure.persistence.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CatalogEntryJpaRepository
        extends JpaRepository<CatalogEntryJpaEntity, UUID> {
    Optional<CatalogEntryJpaEntity> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
}
