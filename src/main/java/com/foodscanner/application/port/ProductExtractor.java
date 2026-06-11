package com.foodscanner.application.port;

import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;

/**
 * Слой: application (порт). Извлечение структурных полей продукта.
 * Адаптеры: StubProductExtractor (сейчас) · TextLlm/ImageLlm (позже, после решения по хостингу).
 * Выбор адаптера — флаг product.extractor=stub|text_llm|image_llm.
 */
public interface ProductExtractor {

    /** @param ocrRawText сырой текст OCR (для TEXT_EXTRACTION); для image-fallback может игнорироваться. */
    ExtractionResult extract(ProductExtractionJob job, String ocrRawText);
}
