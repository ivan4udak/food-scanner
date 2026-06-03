package com.foodscanner.application.usecase;

import com.foodscanner.application.command.CompleteCatalogCommand;
import com.foodscanner.application.result.CompleteCatalogResult;

public interface CompleteCatalogUseCase {
    CompleteCatalogResult execute(CompleteCatalogCommand command);
}
