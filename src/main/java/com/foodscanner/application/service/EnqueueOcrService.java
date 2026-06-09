package com.foodscanner.application.service;

import com.foodscanner.application.usecase.EnqueueOcrUseCase;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Слой: application.
 * Создаёт OCR-задачу (QUEUED) для фото с текстом. Публикация в очередь — следующий срез.
 */
@Service
public class EnqueueOcrService implements EnqueueOcrUseCase {

    /** Текст для распознавания — только на этих фото. */
    private static final Set<String> TEXT_TYPES = Set.of("INGREDIENTS", "NUTRITION");

    private final OcrJobRepository repository;

    public EnqueueOcrService(OcrJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID draftId, String storageKey, String photoType) {
        if (photoType == null || !TEXT_TYPES.contains(photoType.toUpperCase())) return;
        repository.save(OcrJob.queued(draftId, storageKey, photoType.toUpperCase()));
    }
}
