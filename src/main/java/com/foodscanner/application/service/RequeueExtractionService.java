package com.foodscanner.application.service;

import com.foodscanner.application.usecase.RequeueExtractionUseCase;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * Requeue: создаёт новую QUEUED-задачу с тем же ocrJobId/barcode/type; старую оставляем как historical.
 * Разрешено только для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5) — иначе IllegalStateException (→409).
 */
@Service
public class RequeueExtractionService implements RequeueExtractionUseCase {

    private final ProductExtractionJobRepository repository;

    public RequeueExtractionService(ProductExtractionJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> execute(UUID jobId) {
        return repository.findById(jobId).map(old -> {
            int code = old.status().code();
            boolean requeueable = code == ExtractionStatus.NEEDS_REVIEW.code()
                || code == ExtractionStatus.FAILED.code()
                || code == ExtractionStatus.SKIPPED.code();
            if (!requeueable) {
                throw new IllegalStateException(
                    "requeue только для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5), текущий статус=" + code);
            }
            ProductExtractionJob fresh =
                repository.save(ProductExtractionJob.queued(old.ocrJobId(), old.barcode(), old.type()));
            return fresh.id();
        });
    }
}
