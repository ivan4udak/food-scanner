package com.foodscanner.application.service;

import com.foodscanner.application.usecase.EnqueueProductExtractionUseCase;
import com.foodscanner.domain.model.extraction.ExtractionType;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.policy.ExtractionEligibilityPolicy;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * Создаёт задачу извлечения по eligibility-политике (пороги — из конфигурации).
 * Под флагом product.extraction.enabled (по умолчанию ВКЛ — создание дёшево, обработка — ночью).
 */
@Service
public class EnqueueProductExtractionService implements EnqueueProductExtractionUseCase {

    private final ProductExtractionJobRepository repository;
    private final ExtractionEligibilityPolicy policy;
    private final boolean enabled;

    public EnqueueProductExtractionService(
            ProductExtractionJobRepository repository,
            @Value("${product.extraction.enabled:true}") boolean enabled,
            @Value("${product.extraction.min-raw-text-length:100}") int minRawTextLength,
            @Value("${product.extraction.min-ocr-confidence:0.35}") double minConfidence) {
        this.repository = repository;
        this.enabled = enabled;
        this.policy = new ExtractionEligibilityPolicy(minRawTextLength, minConfidence);
    }

    @Override
    public void onOcrResult(UUID ocrJobId, OcrStatus ocrStatus, String rawText, Double confidence, String barcode) {
        if (!enabled) return;
        int len = rawText == null ? 0 : rawText.length();
        Optional<ExtractionType> type = policy.decide(ocrStatus, len, confidence);
        type.ifPresent(t -> repository.save(ProductExtractionJob.queued(ocrJobId, barcode, t)));
    }
}
