package com.foodscanner.infrastructure.persistence.telemetry;

import com.foodscanner.domain.model.telemetry.ClientActivity;
import com.foodscanner.domain.repository.ClientActivityRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Слой: infrastructure.
 * Адаптер ClientActivityRepository → JPA.
 */
@Repository
public class ClientActivityRepositoryAdapter implements ClientActivityRepository {

    private final ClientActivityJpaRepository jpa;

    public ClientActivityRepositoryAdapter(ClientActivityJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ClientActivity a) {
        jpa.save(new ClientActivityJpaEntity(
            a.id(), a.contributorId(), a.sessionId(), a.screen(), a.online(), a.occurredAt(), a.receivedAt()));
    }

    @Override
    public int deleteOlderThan(Instant threshold) {
        return jpa.deleteByOccurredAtBefore(threshold);
    }
}
