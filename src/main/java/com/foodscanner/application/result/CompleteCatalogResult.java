package com.foodscanner.application.result;

import java.util.UUID;

/**
 * Слой: application
 * Результат CompleteCatalogUseCase.
 * Содержит id созданной CatalogEntry и обновлённый счётчик контрибьютора.
 */
public final class CompleteCatalogResult {

    private final UUID catalogEntryId;
    private final int  contributorCompletedCount;

    public CompleteCatalogResult(UUID catalogEntryId, int contributorCompletedCount) {
        this.catalogEntryId            = catalogEntryId;
        this.contributorCompletedCount = contributorCompletedCount;
    }

    public UUID getCatalogEntryId()            { return catalogEntryId; }
    public int  getContributorCompletedCount() { return contributorCompletedCount; }
}
