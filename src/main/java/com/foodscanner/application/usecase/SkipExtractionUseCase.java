package com.foodscanner.application.usecase;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (use case).
 * Админ пропускает задачу извлечения (→ SKIPPED), чтобы воркер её больше не брал.
 * Разрешено для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4).
 */
public interface SkipExtractionUseCase {

    /** @return id той же задачи; empty — не найдена; IllegalStateException — статус не позволяет. */
    Optional<UUID> execute(UUID jobId);
}
