package com.foodscanner.infrastructure.persistence.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ClientLogJpaRepository extends JpaRepository<ClientLogJpaEntity, UUID> {

    @Modifying
    @Query("delete from ClientLogJpaEntity e where e.level not in :levels and e.timestamp < :threshold")
    int deleteByLevelNotInAndTimestampBefore(@Param("levels") Collection<String> levels,
                                             @Param("threshold") Instant threshold);

    @Modifying
    @Query("delete from ClientLogJpaEntity e where e.level in :levels and e.timestamp < :threshold")
    int deleteByLevelInAndTimestampBefore(@Param("levels") Collection<String> levels,
                                          @Param("threshold") Instant threshold);
}
