package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain (порт). Хранилище OCR-задач.
 */
public interface OcrJobRepository {
    OcrJob save(OcrJob job);

    Optional<OcrJob> findById(UUID id);

    List<OcrJob> findByDraftId(UUID draftId);

    /** Количество задач по каждому статусу (для наблюдаемости). Отсутствующие статусы → 0. */
    Map<OcrStatus, Long> countByStatus();

    // ── Жизненный цикл (v1.10.4) ────────────────────────────────────────
    /** Пометить прошлые активные задачи (draft, photoType) замещёнными новой → их число. */
    int supersedePrevious(UUID draftId, String photoType, UUID newJobId);

    /** Пометить orphan активные задачи без catalog_entry с исчезнувшим черновиком → их число. */
    int markOrphans();

    /** Зафиксировать успешную публикацию в очередь (published_at, attempts+1). */
    void markPublished(UUID id);

    /** Зафиксировать неудачу публикации (attempts+1, текст ошибки). */
    void recordPublishFailure(UUID id, String error);

    /** Активные QUEUED, не опубликованные — кандидаты на повторную публикацию. */
    List<OcrJob> findRepublishable();

    /** Размер очереди: активные QUEUED. */
    long countActiveQueued();

    /** Время создания старейшей активной QUEUED-задачи (для метрики возраста очереди). */
    Optional<Instant> oldestQueuedCreatedAt();
}
