package com.foodscanner.infrastructure.extraction;

import com.foodscanner.application.port.ProductExtractor;
import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Слой: infrastructure.
 * Заглушка извлечения (product.extractor=stub, по умолчанию): ничего не извлекает,
 * безопасно проходит pipeline → worker ставит SKIPPED. Реальные LLM/vision — отдельный срез.
 */
@Component
@ConditionalOnProperty(name = "product.extractor.engine", havingValue = "stub", matchIfMissing = true)
public class StubProductExtractor implements ProductExtractor {

    @Override
    public ExtractionResult extract(ProductExtractionJob job, String ocrRawText) {
        return ExtractionResult.empty("STUB");
    }
}
