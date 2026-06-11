package com.foodscanner.application.service;

import com.foodscanner.application.usecase.SkipExtractionUseCase;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * Skip: переводит задачу в SKIPPED (воркер берёт только QUEUED → больше не возьмёт).
 * Разрешено только для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4) — иначе IllegalStateException (→409).
 */
@Service
public class SkipExtractionService implements SkipExtractionUseCase {

    private static final String DEFAULT_REASON = "Skipped by admin";

    private final ProductExtractionJobRepository repository;

    public SkipExtractionService(ProductExtractionJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> execute(UUID jobId) {
        return repository.findById(jobId).map(old -> {
            int code = old.status().code();
            boolean skippable = code == ExtractionStatus.QUEUED.code()
                || code == ExtractionStatus.NEEDS_REVIEW.code()
                || code == ExtractionStatus.FAILED.code();
            if (!skippable) {
                throw new IllegalStateException(
                    "skip только для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4), текущий статус=" + code);
            }
            repository.skip(jobId, DEFAULT_REASON);
            return old.id();
        });
    }
}
