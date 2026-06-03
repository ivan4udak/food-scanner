package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.CatalogDraftStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Repository Interface (Port)
 *
 * findOpenByBarcodeAndContributor нужен для StartCatalogDraftUseCase —
 * не создавать второй черновик если OPEN уже существует.
 */
public interface CatalogDraftRepository {
    CatalogDraft save(CatalogDraft draft);
    Optional<CatalogDraft> findById(UUID id);
    Optional<CatalogDraft> findOpenByBarcodeAndContributor(Barcode barcode, UUID contributorId);
}
