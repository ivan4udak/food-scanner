package com.foodscanner.domain.exception;

import com.foodscanner.domain.model.PhotoType;
import java.util.Set;

/**
 * Слой: domain
 * Бросается CatalogCompletionPolicy когда черновик нельзя завершить.
 * Содержит точный список недостающих типов фото — для информативного ответа API.
 */
public class CatalogNotCompletableException extends RuntimeException {

    private final Set<PhotoType> missingTypes;

    public CatalogNotCompletableException(Set<PhotoType> missingTypes) {
        super("Catalog draft cannot be completed. Missing photo types: " + missingTypes);
        this.missingTypes = missingTypes;
    }

    public Set<PhotoType> getMissingTypes() {
        return missingTypes;
    }
}
