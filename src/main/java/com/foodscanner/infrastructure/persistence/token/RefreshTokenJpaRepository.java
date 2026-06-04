package com.foodscanner.infrastructure.persistence.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
    void deleteByContributorId(UUID contributorId);

    @Modifying
    @Query("delete from RefreshTokenJpaEntity t where t.expiresAt < :now")
    int deleteByExpiresAtBefore(Instant now);
}
