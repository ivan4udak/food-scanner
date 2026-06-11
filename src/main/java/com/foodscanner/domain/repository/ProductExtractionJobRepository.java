package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;

import java.util.List;
import java.util.UUID;

/**
 * Слой: domain (порт). Хранилище задач структурного извлечения.
 */
public interface ProductExtractionJobRepository {
    ProductExtractionJob save(ProductExtractionJob job);

    /** Старейшие QUEUED задачи (для ночного воркера). */
    List<ProductExtractionJob> findQueued(int limit);

    /** Перевести в IN_PROGRESS (attempts+1). */
    void markInProgress(UUID id);

    /** Применить результат извлечения + финальный статус (STRUCTURED/NEEDS_REVIEW/SKIPPED). */
    void applyResult(UUID id, ExtractionStatus status, ExtractionResult result);

    /** Техническая ошибка → FAILED. */
    void markFailed(UUID id, String error);
}
