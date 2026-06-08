package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.telemetry.ServerEvent;

import java.time.Instant;

/**
 * Слой: domain (порт).
 * Хранилище серверных бизнес-событий.
 */
public interface ServerEventRepository {

    void save(ServerEvent event);

    /** Чистка событий старше указанного момента. Возвращает число удалённых. */
    int deleteOlderThan(Instant threshold);
}
