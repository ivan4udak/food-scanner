package com.foodscanner.application.usecase;

import com.foodscanner.application.command.RecordClientSessionCommand;

/**
 * Слой: application (use case).
 * Регистрация/обновление клиентской сессии.
 */
public interface RecordClientSessionUseCase {
    void execute(RecordClientSessionCommand command);
}
