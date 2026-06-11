package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain (порт). Хранилище задач структурного извлечения.
 */
public interface ProductExtractionJobRepository {
    ProductExtractionJob save(ProductExtractionJob job);

    /** Задача по id (для админ-действий requeue/skip). */
    Optional<ProductExtractionJob> findById(UUID id);

    /** Старейшие QUEUED задачи (для ночного воркера). */
    List<ProductExtractionJob> findQueued(int limit);

    /** Перевести в IN_PROGRESS (attempts+1). */
    void markInProgress(UUID id);

    /** Применить результат извлечения + финальный статус (STRUCTURED/NEEDS_REVIEW/SKIPPED). */
    void applyResult(UUID id, ExtractionStatus status, ExtractionResult result);

    /** Техническая ошибка → FAILED. */
    void markFailed(UUID id, String error);

    /** Пропустить задачу админом → SKIPPED (processedAt=now, lastError=reason). */
    void skip(UUID id, String reason);

    /** Количество задач по каждому статусу (для метрик; без zero-fill). */
    Map<ExtractionStatus, Long> countByStatus();

    /** Размер очереди — количество QUEUED задач. */
    long countQueued();

    /** queued_at старейшей QUEUED задачи (возраст очереди) — пусто, если очередь пуста. */
    Optional<Instant> oldestQueuedAt();
}
