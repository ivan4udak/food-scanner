package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.CatalogDraftStatus;
import java.time.Instant;
import java.util.List;
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

    /** Незавершённые черновики (OPEN/ABANDONED) старше cutoff — для очистки мусора (Блок 15). */
    List<CatalogDraft> findStaleUnfinished(Instant cutoff);

    /** Удаляет черновик (draft_photos удаляются каскадно). */
    void deleteById(UUID id);
}
