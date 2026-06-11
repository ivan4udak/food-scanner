package com.foodscanner.application.usecase;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (use case).
 * Админ переотправляет задачу извлечения: создаёт новую QUEUED для того же OCR-источника.
 * Разрешено для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5).
 */
public interface RequeueExtractionUseCase {

    /** @return id новой QUEUED-задачи; empty — задача не найдена; IllegalStateException — статус не позволяет. */
    Optional<UUID> execute(UUID jobId);
}
