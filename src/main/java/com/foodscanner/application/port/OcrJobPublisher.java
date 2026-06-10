package com.foodscanner.application.port;

import com.foodscanner.domain.model.ocr.OcrJob;

/**
 * Слой: application (порт). Публикация OCR-задачи в очередь.
 * Реализации: RabbitMQ (под флагом) либо No-op (по умолчанию).
 */
public interface OcrJobPublisher {
    /** @return true, если задача реально опубликована в брокер (false — no-op/ошибка → останется QUEUED). */
    boolean publish(OcrJob job);
}
