package com.foodscanner.application.service;

import com.foodscanner.application.command.AddDraftPhotoCommand;
import com.foodscanner.application.result.AddDraftPhotoResult;
import com.foodscanner.application.usecase.AddDraftPhotoUseCase;
import com.foodscanner.domain.exception.CatalogDraftNotFoundException;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.PhotoType;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogDraftRepository;

import java.util.Set;

/**
 * Слой: application
 * Тип: Use Case Implementation
 *
 * Оркестрация:
 *   1. Найти черновик по id
 *   2. Проверить что он принадлежит контрибьютору
 *   3. Добавить фото через агрегат
 *   4. Сохранить черновик
 *   5. Вернуть прогресс через policy.findMissing()
 *
 * Зависимости: CatalogDraftRepository, CatalogCompletionPolicy.
 */
public class AddDraftPhotoService implements AddDraftPhotoUseCase {

    private final CatalogDraftRepository  draftRepository;
    private final CatalogCompletionPolicy policy;

    public AddDraftPhotoService(CatalogDraftRepository draftRepository,
                                CatalogCompletionPolicy policy) {
        this.draftRepository = draftRepository;
        this.policy          = policy;
    }

    @Override
    public AddDraftPhotoResult execute(AddDraftPhotoCommand command) {
        CatalogDraft draft = draftRepository.findById(command.getDraftId())
            .orElseThrow(() -> new CatalogDraftNotFoundException(command.getDraftId()));

        if (!draft.getContributorId().equals(command.getContributorId())) {
            throw new IllegalStateException(
                "Draft " + command.getDraftId() + " does not belong to contributor "
                + command.getContributorId());
        }

        draft.addPhoto(command.getPhotoType(), command.getStorageKey());
        draftRepository.save(draft);

        Set<PhotoType> missing      = policy.findMissing(draft);
        int            uploadedCount = CatalogCompletionPolicy.REQUIRED_TYPES.size() - missing.size();

        return new AddDraftPhotoResult(
            uploadedCount,
            CatalogCompletionPolicy.REQUIRED_TYPES.size(),
            missing, false);
    }
}
