package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.telemetry.ClientSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain (порт).
 * Хранилище клиентских сессий. Одна строка на sessionId.
 */
public interface ClientSessionRepository {

    Optional<ClientSession> findBySessionId(UUID sessionId);

    /** Создаёт или обновляет сессию (по sessionId). */
    void upsert(ClientSession session);

    /** Обновляет last_seen_at сессии, если она существует. */
    void touch(UUID sessionId, Instant lastSeenAt);
}
