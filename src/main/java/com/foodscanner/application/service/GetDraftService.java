package com.foodscanner.application.service;

import com.foodscanner.application.result.DraftDetailsResult;
import com.foodscanner.application.usecase.GetDraftUseCase;
import com.foodscanner.domain.exception.CatalogDraftNotFoundException;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.DraftPhoto;
import com.foodscanner.domain.model.PhotoType;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogDraftRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Слой: application
 * Тип: Use Case Implementation
 *
 * Возвращает состояние черновика владельца: эффективные фото (по одному, последнему,
 * на тип), прогресс и недостающие типы. Для восстановления экрана черновика на клиенте.
 */
public class GetDraftService implements GetDraftUseCase {

    private final CatalogDraftRepository  draftRepository;
    private final CatalogCompletionPolicy policy;

    public GetDraftService(CatalogDraftRepository draftRepository, CatalogCompletionPolicy policy) {
        this.draftRepository = draftRepository;
        this.policy          = policy;
    }

    @Override
    public DraftDetailsResult execute(UUID draftId, UUID contributorId) {
        CatalogDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new CatalogDraftNotFoundException(draftId));

        if (!draft.getContributorId().equals(contributorId)) {
            throw new IllegalStateException(
                "Draft " + draftId + " does not belong to contributor " + contributorId);
        }

        // Эффективный набор: последнее фото каждого типа (контрибьютор мог перефотографировать).
        Map<PhotoType, DraftPhoto> latestByType = new EnumMap<>(PhotoType.class);
        for (DraftPhoto p : draft.getPhotos()) {
            latestByType.put(p.getType(), p);
        }
        List<DraftDetailsResult.Photo> photos = latestByType.values().stream()
            .map(p -> new DraftDetailsResult.Photo(p.getType(), p.getStorageKey(), p.getCapturedAt()))
            .toList();

        Set<PhotoType> missing       = policy.findMissing(draft);
        int            uploadedCount = CatalogCompletionPolicy.REQUIRED_TYPES.size() - missing.size();

        return new DraftDetailsResult(
            draft.getId(),
            draft.getBarcode().getValue(),
            draft.getStatus().name(),
            photos,
            uploadedCount,
            CatalogCompletionPolicy.REQUIRED_TYPES.size(),
            missing,
            missing.isEmpty());
    }
}
