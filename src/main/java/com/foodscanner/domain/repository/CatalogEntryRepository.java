package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogEntry;
import java.util.Optional;
import java.util.UUID;

public interface CatalogEntryRepository {
    CatalogEntry save(CatalogEntry entry);
    Optional<CatalogEntry> findById(UUID id);
    Optional<CatalogEntry> findByBarcode(Barcode barcode);
    boolean existsByBarcode(Barcode barcode);
}
