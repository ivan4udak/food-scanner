package com.foodscanner.infrastructure.messaging;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.domain.model.ocr.OcrJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Слой: infrastructure.
 * Публикатор-заглушка (брокер выключен). Задача остаётся QUEUED в БД до включения OCR.
 */
@Component
@ConditionalOnProperty(name = "ocr.amqp.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOcrJobPublisher implements OcrJobPublisher {
    @Override
    public void publish(OcrJob job) {
        // no-op
    }
}
