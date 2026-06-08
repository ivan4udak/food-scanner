package com.foodscanner.infrastructure.persistence.telemetry;

import com.foodscanner.domain.model.telemetry.ServerEvent;
import com.foodscanner.domain.repository.ServerEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Слой: infrastructure.
 * Адаптер ServerEventRepository → JPA.
 */
@Repository
public class ServerEventRepositoryAdapter implements ServerEventRepository {

    private final ServerEventJpaRepository jpa;

    public ServerEventRepositoryAdapter(ServerEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ServerEvent e) {
        jpa.save(new ServerEventJpaEntity(
            e.id(), e.occurredAt(), e.level(), e.event(), e.correlationId(), e.requestId(), e.contributorId(),
            e.username(), e.sessionId(), e.method(), e.path(), e.httpStatus(), e.durationMs(), e.useCase(),
            e.barcode(), e.draftId(), e.catalogEntryId(), e.photoId(), e.errorCode(), e.errorMessage(),
            e.exceptionClass(), e.metadata()));
    }

    @Override
    public int deleteOlderThan(Instant threshold) {
        return jpa.deleteByOccurredAtBefore(threshold);
    }
}
