package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.ocr.OcrJob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain (порт). Хранилище OCR-задач.
 */
public interface OcrJobRepository {
    OcrJob save(OcrJob job);

    Optional<OcrJob> findById(UUID id);

    List<OcrJob> findByDraftId(UUID draftId);
}
