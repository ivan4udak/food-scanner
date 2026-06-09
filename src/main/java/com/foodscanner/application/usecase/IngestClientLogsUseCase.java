package com.foodscanner.application.usecase;

import com.foodscanner.application.command.IngestClientLogsCommand;
import com.foodscanner.application.result.IngestClientLogsResult;

/**
 * Слой: application (use case).
 * Приём партии клиентских логов: маскировка секретов, отсев heartbeat-шума, сохранение.
 */
public interface IngestClientLogsUseCase {
    IngestClientLogsResult execute(IngestClientLogsCommand command);
}
