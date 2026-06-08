package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.telemetry.ClientLogEntry;

import java.time.Instant;
import java.util.List;

/**
 * Слой: domain (порт).
 * Хранилище клиентских логов.
 */
public interface ClientLogRepository {

    void saveAll(List<ClientLogEntry> entries);

    /** Удаляет логи не-WARN/ERROR старше указанного момента. Возвращает число удалённых. */
    int deleteRoutineOlderThan(Instant threshold);

    /** Удаляет WARN/ERROR логи старше указанного момента. Возвращает число удалённых. */
    int deleteImportantOlderThan(Instant threshold);
}
