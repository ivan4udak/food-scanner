package com.foodscanner.application.usecase;

import java.util.UUID;

/**
 * Слой: application (use case).
 * Пользователь скрывает/показывает себя в публичном рейтинге (opt-out).
 */
public interface SetLeaderboardVisibilityUseCase {
    /** @return актуальное значение «скрыт ли». */
    boolean execute(UUID contributorId, boolean hidden);
}
