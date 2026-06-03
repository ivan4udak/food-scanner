package com.foodscanner.application.result;

import com.foodscanner.domain.model.PhotoType;
import java.util.Set;

/**
 * Слой: application
 * Результат AddDraftPhotoUseCase.
 *
 * Возвращает текущий прогресс: сколько из 6 фото загружено
 * и каких типов ещё не хватает — для прогресс-бара в UI.
 */
public final class AddDraftPhotoResult {

    private final int           uploadedCount;
    private final int           requiredCount;
    private final Set<PhotoType> missingTypes;

    public AddDraftPhotoResult(int uploadedCount, int requiredCount,
                               Set<PhotoType> missingTypes, boolean complete) {
        this.uploadedCount = uploadedCount;
        this.requiredCount = requiredCount;
        this.missingTypes  = missingTypes;
    }

    public int            getUploadedCount() { return uploadedCount; }
    public int            getRequiredCount() { return requiredCount; }
    public Set<PhotoType> getMissingTypes()  { return missingTypes; }
    public boolean        isComplete()       { return missingTypes.isEmpty(); }
}
