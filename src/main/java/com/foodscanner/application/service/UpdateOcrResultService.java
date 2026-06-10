package com.foodscanner.application.service;

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
 * (draftId/storageKey/photoType/createdAt) сохраняются.
 */
@Service
public class UpdateOcrResultService implements UpdateOcrResultUseCase {

    private final OcrJobRepository repository;

    public UpdateOcrResultService(OcrJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID jobId, OcrStatus status, String rawText, String parsedIngredients,
                        String parsedNutrition, Double confidence, String errorCode, String errorMessage) {
        repository.findById(jobId).ifPresent(j -> repository.save(new OcrJob(
            j.id(), j.draftId(), j.catalogEntryId(), j.storageKey(), j.photoType(),
            status, j.attempts() + 1, rawText, parsedIngredients, parsedNutrition,
            confidence, errorCode, errorMessage, j.createdAt(), Instant.now())));
    }
}
