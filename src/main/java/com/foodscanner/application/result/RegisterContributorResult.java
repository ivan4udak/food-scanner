package com.foodscanner.application.result;

import java.util.UUID;

/**
 * Слой: application
 * Результат RegisterContributorUseCase.
 * Use case не возвращает domain объект — это защищает модель от случайного изменения.
 */
public final class RegisterContributorResult {
    private final UUID   contributorId;
    private final String nickname;

    public RegisterContributorResult(UUID contributorId, String nickname) {
        this.contributorId = contributorId;
        this.nickname      = nickname;
    }

    public UUID   getContributorId() { return contributorId; }
    public String getNickname()      { return nickname; }
}
