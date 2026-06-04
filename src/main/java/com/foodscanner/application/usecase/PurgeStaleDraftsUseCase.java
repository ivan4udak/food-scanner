package com.foodscanner.application.usecase;

import com.foodscanner.application.result.PurgeResult;
import java.time.Duration;

/** Слой: application. Очистка незавершённых черновиков старше maxAge (Блок 15). */
public interface PurgeStaleDraftsUseCase {
    PurgeResult purge(Duration maxAge);
}
