package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain
 * Порт хранилища refresh-токенов.
 */
public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteById(UUID id);
    void deleteByContributorId(UUID contributorId);
    int deleteExpired(Instant now);
}
