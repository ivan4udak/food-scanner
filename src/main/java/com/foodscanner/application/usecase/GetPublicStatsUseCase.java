package com.foodscanner.application.usecase;

import com.foodscanner.application.result.PublicStatsResult;

/**
 * Слой: application (use case).
 * Публичная статистика проекта (без авторизации).
 */
public interface GetPublicStatsUseCase {
    PublicStatsResult execute();
}
