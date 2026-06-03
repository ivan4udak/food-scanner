package com.foodscanner.domain.exception;

import java.util.UUID;

public class CatalogDraftNotFoundException extends RuntimeException {
    public CatalogDraftNotFoundException(UUID draftId) {
        super("Catalog draft not found: " + draftId);
    }
}
