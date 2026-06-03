package com.foodscanner.application.service;

import com.foodscanner.application.result.FindCatalogEntryResult;
import com.foodscanner.application.usecase.FindCatalogEntryByBarcodeUseCase;
import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.repository.CatalogEntryRepository;

/**
 * Слой: application
 * Тип: Use Case Implementation (Query)
 *
 * Возвращает null если не найдено — осознанное решение для MVP
 * вместо Optional или исключения. API-слой сам решает что вернуть (404 vs 200).
 *
 * Расширение: при появлении поиска по названию — добавить
 * FindCatalogEntriesByNameUseCase отдельным интерфейсом.
 */
public class FindCatalogEntryByBarcodeService implements FindCatalogEntryByBarcodeUseCase {

    private final CatalogEntryRepository entryRepository;

    public FindCatalogEntryByBarcodeService(CatalogEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Override
    public FindCatalogEntryResult execute(String barcodeValue) {
        return entryRepository.findByBarcode(new Barcode(barcodeValue))
            .map(FindCatalogEntryResult::from)
            .orElse(null);
    }
}
