package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;

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
}
