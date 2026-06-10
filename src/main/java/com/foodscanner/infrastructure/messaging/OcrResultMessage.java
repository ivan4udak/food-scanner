package com.foodscanner.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Сообщение результата из очереди ocr.results (см. docs/OCR.md). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResultMessage(
        String jobId,
        Integer status,
        String rawText,
        String parsedIngredients,
        String parsedNutrition,
        Double confidence,
        String errorCode,
        String errorMessage
) {}
