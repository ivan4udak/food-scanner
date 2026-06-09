package com.foodscanner.application.usecase;

import com.foodscanner.application.command.RecordServerEventCommand;

/**
 * Слой: application (use case).
 * Запись значимого серверного события (с отсевом heartbeat-шума и маскировкой).
 */
public interface RecordServerEventUseCase {
    void execute(RecordServerEventCommand command);
}
