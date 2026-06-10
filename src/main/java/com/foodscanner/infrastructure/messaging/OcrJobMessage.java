package com.foodscanner.infrastructure.messaging;

/** Сообщение задачи в очередь ocr.jobs (см. docs/OCR.md). */
public record OcrJobMessage(
        String jobId,
        String storageKey,
        String photoType,
        String draftId,
        int attempt
) {}
