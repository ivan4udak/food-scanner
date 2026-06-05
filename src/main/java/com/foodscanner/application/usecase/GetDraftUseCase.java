package com.foodscanner.application.usecase;

import com.foodscanner.application.result.DraftDetailsResult;

import java.util.UUID;

/** Состояние черновика (для восстановления фото на клиенте). */
public interface GetDraftUseCase {
    DraftDetailsResult execute(UUID draftId, UUID contributorId);
}
