package com.foodscanner.application.usecase;

import com.foodscanner.application.command.AddDraftPhotoCommand;
import com.foodscanner.application.result.AddDraftPhotoResult;

public interface AddDraftPhotoUseCase {
    AddDraftPhotoResult execute(AddDraftPhotoCommand command);
}
