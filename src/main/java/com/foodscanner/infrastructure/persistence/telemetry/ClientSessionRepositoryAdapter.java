package com.foodscanner.infrastructure.persistence.telemetry;

import com.foodscanner.domain.model.telemetry.ClientSession;
import com.foodscanner.domain.repository.ClientSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: infrastructure.
 * Адаптер ClientSessionRepository → JPA.
 */
@Repository
public class ClientSessionRepositoryAdapter implements ClientSessionRepository {

    private final ClientSessionJpaRepository jpa;

    public ClientSessionRepositoryAdapter(ClientSessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ClientSession> findBySessionId(UUID sessionId) {
        return jpa.findBySessionId(sessionId).map(ClientSessionRepositoryAdapter::toDomain);
    }

    @Override
    public void upsert(ClientSession session) {
        jpa.save(toJpa(session));
    }

    @Override
    public void touch(UUID sessionId, Instant lastSeenAt) {
        jpa.touchLastSeen(sessionId, lastSeenAt);
    }

    private static ClientSession toDomain(ClientSessionJpaEntity e) {
        return ClientSession.builder()
            .id(e.getId())
            .contributorId(e.getContributorId())
            .sessionId(e.getSessionId())
            .startedAt(e.getStartedAt())
            .lastSeenAt(e.getLastSeenAt())
            .clientVersion(e.getClientVersion())
            .pwaVersion(e.getPwaVersion())
            .browser(e.getBrowser())
            .os(e.getOs())
            .deviceType(e.getDeviceType())
            .language(e.getLanguage())
            .timezone(e.getTimezone())
            .screenWidth(e.getScreenWidth())
            .screenHeight(e.getScreenHeight())
            .hardwareConcurrency(e.getHardwareConcurrency())
            .deviceMemory(e.getDeviceMemory())
            .networkStatus(e.getNetworkStatus())
            .standalone(e.getStandalone())
            .build();
    }

    private static ClientSessionJpaEntity toJpa(ClientSession s) {
        return new ClientSessionJpaEntity(
            s.id(), s.contributorId(), s.sessionId(), s.startedAt(), s.lastSeenAt(), s.clientVersion(),
            s.pwaVersion(), s.browser(), s.os(), s.deviceType(), s.language(), s.timezone(),
            s.screenWidth(), s.screenHeight(), s.hardwareConcurrency(), s.deviceMemory(),
            s.networkStatus(), s.standalone());
    }
}
