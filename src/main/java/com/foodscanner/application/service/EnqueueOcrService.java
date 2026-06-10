package com.foodscanner.application.service;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.application.usecase.EnqueueOcrUseCase;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Слой: application.
 * Создаёт OCR-задачу (QUEUED) для фото с текстом и публикует её в очередь
 * (NoOp-публикатор, пока брокер выключен).
 */
@Service
public class EnqueueOcrService implements EnqueueOcrUseCase {

    /** Текст для распознавания — только на этих фото. */
    private static final Set<String> TEXT_TYPES = Set.of("INGREDIENTS", "NUTRITION");

    private final OcrJobRepository repository;
    private final OcrJobPublisher publisher;

    public EnqueueOcrService(OcrJobRepository repository, OcrJobPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public void execute(UUID draftId, String storageKey, String photoType) {
        if (photoType == null || !TEXT_TYPES.contains(photoType.toUpperCase())) return;
        OcrJob job = repository.save(OcrJob.queued(draftId, storageKey, photoType.toUpperCase()));
        publisher.publish(job);
    }
}
