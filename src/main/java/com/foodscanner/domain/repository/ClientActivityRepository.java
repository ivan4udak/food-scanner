package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.telemetry.ClientActivity;

import java.time.Instant;

/**
 * Слой: domain (порт).
 * Хранилище событий активности клиента.
 */
public interface ClientActivityRepository {

    void save(ClientActivity activity);

    /** Чистка старых записей активности. Возвращает число удалённых. */
    int deleteOlderThan(Instant threshold);
}
