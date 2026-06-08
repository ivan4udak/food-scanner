package com.foodscanner.infrastructure.persistence.telemetry;

import com.foodscanner.domain.model.telemetry.ClientLogEntry;
import com.foodscanner.domain.repository.ClientLogRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Слой: infrastructure.
 * Адаптер ClientLogRepository → JPA. Retention: WARN/ERROR хранятся дольше.
 */
@Repository
public class ClientLogRepositoryAdapter implements ClientLogRepository {

    private static final Set<String> IMPORTANT_LEVELS = Set.of("WARN", "ERROR");

    private final ClientLogJpaRepository jpa;

    public ClientLogRepositoryAdapter(ClientLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void saveAll(List<ClientLogEntry> entries) {
        jpa.saveAll(entries.stream().map(ClientLogRepositoryAdapter::toJpa).toList());
    }

    @Override
    public int deleteRoutineOlderThan(Instant threshold) {
        return jpa.deleteByLevelNotInAndTimestampBefore(IMPORTANT_LEVELS, threshold);
    }

    @Override
    public int deleteImportantOlderThan(Instant threshold) {
        return jpa.deleteByLevelInAndTimestampBefore(IMPORTANT_LEVELS, threshold);
    }

    private static ClientLogJpaEntity toJpa(ClientLogEntry e) {
        return new ClientLogJpaEntity(
            e.id(), e.contributorId(), e.sessionId(), e.clientLogId(), e.correlationId(), e.requestId(),
            e.timestamp(), e.receivedAt(), e.level(), e.category(), e.event(), e.message(), e.screen(),
            e.metadata(), e.durationMs(), e.stackTrace(), e.barcode(), e.draftId(), e.catalogEntryId(),
            e.photoId(), e.apiMethod(), e.apiPath(), e.httpStatus(), e.clientVersion(), e.pwaVersion());
    }
}
