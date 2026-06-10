package com.foodscanner.application.usecase;

import com.foodscanner.domain.model.ocr.OcrStatus;

import java.util.UUID;

/**
 * Слой: application (use case). Применение результата OCR к задаче.
 */
public interface UpdateOcrResultUseCase {
    void execute(UUID jobId, OcrStatus status, String rawText, String parsedIngredients,
                 String parsedNutrition, Double confidence, String errorCode, String errorMessage);
}
