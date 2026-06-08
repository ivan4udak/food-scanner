package com.foodscanner.application.usecase;

import com.foodscanner.application.command.RecordClientActivityCommand;

/**
 * Слой: application (use case).
 * Запись активности клиента + обновление last_seen сессии.
 */
public interface RecordClientActivityUseCase {
    void execute(RecordClientActivityCommand command);
}
