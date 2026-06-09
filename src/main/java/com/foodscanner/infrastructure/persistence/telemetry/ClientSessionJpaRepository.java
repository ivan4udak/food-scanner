package com.foodscanner.infrastructure.persistence.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClientSessionJpaRepository extends JpaRepository<ClientSessionJpaEntity, UUID> {

    Optional<ClientSessionJpaEntity> findBySessionId(UUID sessionId);

    @Modifying
    @Query("update ClientSessionJpaEntity e set e.lastSeenAt = :ts where e.sessionId = :sessionId")
    int touchLastSeen(@Param("sessionId") UUID sessionId, @Param("ts") Instant lastSeenAt);
}
