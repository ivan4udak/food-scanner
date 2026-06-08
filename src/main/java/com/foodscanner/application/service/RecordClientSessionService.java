package com.foodscanner.application.service;

import com.foodscanner.application.command.RecordClientSessionCommand;
import com.foodscanner.application.usecase.RecordClientSessionUseCase;
import com.foodscanner.domain.model.telemetry.ClientSession;
import com.foodscanner.domain.repository.ClientSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application.
 * Upsert клиентской сессии: при повторном POST сохраняем исходный startedAt,
 * обновляем last_seen и снимок окружения.
 */
@Service
public class RecordClientSessionService implements RecordClientSessionUseCase {

    private final ClientSessionRepository repository;

    public RecordClientSessionService(ClientSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(RecordClientSessionCommand c) {
        Instant now = c.receivedAt() != null ? c.receivedAt() : Instant.now();
        Instant startedAt = repository.findBySessionId(c.sessionId())
            .map(ClientSession::startedAt)
            .orElse(now);
        UUID id = repository.findBySessionId(c.sessionId())
            .map(ClientSession::id)
            .orElse(UUID.randomUUID());

        ClientSession session = ClientSession.builder()
            .id(id)
            .contributorId(c.contributorId())
            .sessionId(c.sessionId())
            .startedAt(startedAt)
            .lastSeenAt(now)
            .clientVersion(c.clientVersion())
            .pwaVersion(c.pwaVersion())
            .browser(c.browser())
            .os(c.os())
            .deviceType(c.deviceType())
            .language(c.language())
            .timezone(c.timezone())
            .screenWidth(c.screenWidth())
            .screenHeight(c.screenHeight())
            .hardwareConcurrency(c.hardwareConcurrency())
            .deviceMemory(c.deviceMemory())
            .networkStatus(c.networkStatus())
            .standalone(c.standalone())
            .build();

        repository.upsert(session);
    }
}
