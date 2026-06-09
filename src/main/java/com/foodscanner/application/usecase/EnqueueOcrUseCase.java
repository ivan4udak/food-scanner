package com.foodscanner.application.usecase;

import java.util.UUID;

/**
 * Слой: application (use case).
 * Ставит OCR-задачу для фото с текстом (INGREDIENTS/NUTRITION). Прочие типы игнорируются.
 */
public interface EnqueueOcrUseCase {
    void execute(UUID draftId, String storageKey, String photoType);
}
