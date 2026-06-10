package com.foodscanner.infrastructure.messaging;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.domain.model.ocr.OcrJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Слой: infrastructure.
 * Публикация OCR-задачи в RabbitMQ (best-effort: ошибка брокера не ломает загрузку фото —
 * задача уже сохранена как QUEUED и может быть переотправлена позже).
 */
@Component
@ConditionalOnProperty(name = "ocr.amqp.enabled", havingValue = "true")
public class RabbitOcrJobPublisher implements OcrJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOcrJobPublisher.class);

    private final RabbitTemplate rabbit;
    private final String exchange;

    public RabbitOcrJobPublisher(RabbitTemplate rabbit, @Value("${ocr.exchange:ocr}") String exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    @Override
    public boolean publish(OcrJob job) {
        try {
            rabbit.convertAndSend(exchange, "job", new OcrJobMessage(
                job.id().toString(), job.storageKey(), job.photoType(),
                job.draftId() != null ? job.draftId().toString() : null, job.attempts() + 1));
            return true;
        } catch (Exception e) {
            log.warn("OCR job publish failed (job={}): {}", job.id(), e.getMessage());
            return false;
        }
    }
}
