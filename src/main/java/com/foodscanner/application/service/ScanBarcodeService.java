package com.foodscanner.application.service;

import com.foodscanner.application.command.ScanBarcodeCommand;
import com.foodscanner.application.result.ScanBarcodeResult;
import com.foodscanner.application.usecase.ScanBarcodeUseCase;
import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.CatalogEntryRepository;

import java.util.Optional;

/**
 * Слой: application
 * Тип: Use Case Implementation
 *
 * Оркестрация:
 *   1. CatalogEntry существует? → EXISTS, стоп
 *   2. OPEN черновик у контрибьютора уже есть? → NEW + вернуть существующий draftId
 *   3. Иначе → создать новый черновик → NEW + новый draftId
 *
 * Зависимости: CatalogEntryRepository, CatalogDraftRepository (interfaces из domain).
 */
public class ScanBarcodeService implements ScanBarcodeUseCase {

    private final CatalogEntryRepository entryRepository;
    private final CatalogDraftRepository draftRepository;

    public ScanBarcodeService(CatalogEntryRepository entryRepository,
                              CatalogDraftRepository draftRepository) {
        this.entryRepository = entryRepository;
        this.draftRepository = draftRepository;
    }

    @Override
    public ScanBarcodeResult execute(ScanBarcodeCommand command) {
        Barcode barcode = new Barcode(command.getBarcodeValue());

        if (entryRepository.existsByBarcode(barcode)) {
            return ScanBarcodeResult.alreadyExists();
        }

        Optional<CatalogDraft> existingOpen =
            draftRepository.findOpenByBarcodeAndContributor(
                barcode, command.getContributorId());

        if (existingOpen.isPresent()) {
            return ScanBarcodeResult.newProduct(existingOpen.get().getId());
        }

        CatalogDraft newDraft = CatalogDraft.create(barcode, command.getContributorId());
        draftRepository.save(newDraft);

        return ScanBarcodeResult.newProduct(newDraft.getId());
    }
}
