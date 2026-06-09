package com.foodscanner.infrastructure.persistence.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ClientActivityJpaRepository extends JpaRepository<ClientActivityJpaEntity, UUID> {

    @Modifying
    @Query("delete from ClientActivityJpaEntity e where e.occurredAt < :threshold")
    int deleteByOccurredAtBefore(@Param("threshold") Instant threshold);
}
