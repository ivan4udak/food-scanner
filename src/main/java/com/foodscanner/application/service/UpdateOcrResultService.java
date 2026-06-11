package com.foodscanner.application.service;

import com.foodscanner.application.usecase.EnqueueProductExtractionUseCase;
import com.foodscanner.application.usecase.UpdateOcrResultUseCase;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application.
 * Применяет результат OCR к задаче: статус/текст/КБЖУ/ошибка, attempts+1. Прочие поля
 * (draftId/storageKey/photoType/createdAt) сохраняются. После применения — ставит задачу
 * структурного извлечения (по eligibility-политике), которая обрабатывается отдельно (ночью).
 */
@Service
public class UpdateOcrResultService implements UpdateOcrResultUseCase {

    private final OcrJobRepository repository;
    private final EnqueueProductExtractionUseCase enqueueExtraction;

    public UpdateOcrResultService(OcrJobRepository repository,
                                  EnqueueProductExtractionUseCase enqueueExtraction) {
        this.repository = repository;
        this.enqueueExtraction = enqueueExtraction;
    }

    @Override
    public void execute(UUID jobId, OcrStatus status, String rawText, String parsedIngredients,
                        String parsedNutrition, Double confidence, String errorCode, String errorMessage) {
        repository.findById(jobId).ifPresent(j -> {
            repository.save(new OcrJob(
                j.id(), j.draftId(), j.catalogEntryId(), j.storageKey(), j.photoType(),
                status, j.attempts() + 1, rawText, parsedIngredients, parsedNutrition,
                confidence, errorCode, errorMessage, j.createdAt(), Instant.now()));
            // barcode резолвится позже (в админке через draft/entry) — здесь не обязателен
            enqueueExtraction.onOcrResult(jobId, status, rawText, confidence, null);
        });
    }
}
