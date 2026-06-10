package com.foodscanner.application.usecase;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (use case).
 * Повторная отправка OCR-задачи в очередь (когда считаем, что ошибка не в фото):
 * только для статусов PHOTO_UNREADABLE(3) / ERROR(5).
 */
public interface ReprocessOcrUseCase {

    /** @return id новой QUEUED-задачи; Optional.empty() — задача не найдена.
     *  Бросает IllegalStateException, если статус не 3/5. */
    Optional<UUID> execute(UUID jobId);
}
