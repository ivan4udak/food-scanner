package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.extraction.ProductExtractionJob;

/**
 * Слой: domain (порт). Хранилище задач структурного извлечения.
 */
public interface ProductExtractionJobRepository {
    ProductExtractionJob save(ProductExtractionJob job);
}
