package com.foodscanner.application.usecase;

import com.foodscanner.domain.model.ocr.OcrStatus;

import java.util.UUID;

/**
 * Слой: application (use case).
 * По результату OCR создаёт (или нет) задачу структурного извлечения нужного типа.
 */
public interface EnqueueProductExtractionUseCase {

    void onOcrResult(UUID ocrJobId, OcrStatus ocrStatus, String rawText, Double confidence, String barcode);
}
