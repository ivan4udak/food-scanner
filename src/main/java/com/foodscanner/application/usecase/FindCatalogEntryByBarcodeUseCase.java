package com.foodscanner.application.usecase;

import com.foodscanner.application.result.FindCatalogEntryResult;

public interface FindCatalogEntryByBarcodeUseCase {
    FindCatalogEntryResult execute(String barcodeValue);
}
