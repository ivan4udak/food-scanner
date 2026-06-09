package com.foodscanner.application.service;

import com.foodscanner.application.command.RecordClientActivityCommand;
import com.foodscanner.application.usecase.RecordClientActivityUseCase;
import com.foodscanner.domain.model.telemetry.ClientActivity;
import com.foodscanner.domain.repository.ClientActivityRepository;
import com.foodscanner.domain.repository.ClientSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application.
 * Запись активности клиента + обновление last_seen сессии (для online/last-activity).
 */
@Service
public class RecordClientActivityService implements RecordClientActivityUseCase {

    private final ClientActivityRepository activityRepository;
    private final ClientSessionRepository sessionRepository;

    public RecordClientActivityService(ClientActivityRepository activityRepository,
                                       ClientSessionRepository sessionRepository) {
        this.activityRepository = activityRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void execute(RecordClientActivityCommand c) {
        Instant now = Instant.now();
        Instant occurredAt = c.occurredAt() != null ? c.occurredAt() : now;

        activityRepository.save(new ClientActivity(
            UUID.randomUUID(), c.contributorId(), c.sessionId(), c.screen(), c.online(), occurredAt, now));

        sessionRepository.touch(c.sessionId(), now);
    }
}
