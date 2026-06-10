package com.foodscanner.infrastructure.messaging;

import com.foodscanner.application.usecase.UpdateOcrResultUseCase;
import com.foodscanner.domain.model.ocr.OcrStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Слой: infrastructure.
 * Принимает результаты OCR из очереди и применяет их к задаче. Под флагом ocr.amqp.enabled.
 */
@Component
@ConditionalOnProperty(name = "ocr.amqp.enabled", havingValue = "true")
public class OcrResultListener {

    private static final Logger log = LoggerFactory.getLogger(OcrResultListener.class);

    private final UpdateOcrResultUseCase updateResult;

    public OcrResultListener(UpdateOcrResultUseCase updateResult) {
        this.updateResult = updateResult;
    }

    @RabbitListener(queues = "${ocr.results-queue:ocr.results}")
    public void onResult(OcrResultMessage m) {
        if (m == null || m.jobId() == null || m.status() == null) {
            log.warn("OCR result: пустое сообщение, пропуск");
            return;
        }
        try {
            updateResult.execute(UUID.fromString(m.jobId()), OcrStatus.fromCode(m.status()),
                m.rawText(), m.parsedIngredients(), m.parsedNutrition(), m.confidence(),
                m.errorCode(), m.errorMessage());
        } catch (Exception e) {
            log.warn("OCR result apply failed (job={}): {}", m.jobId(), e.getMessage());
        }
    }
}
