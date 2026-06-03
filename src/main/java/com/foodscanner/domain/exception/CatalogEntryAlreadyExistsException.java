package com.foodscanner.domain.exception;

public class CatalogEntryAlreadyExistsException extends RuntimeException {
    public CatalogEntryAlreadyExistsException(String barcode) {
        super("Catalog entry for barcode '" + barcode + "' already exists");
    }
}
