package com.foodscanner.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Aggregate Root
 *
 * Refresh-токен контрибьютора. В БД хранится ТОЛЬКО хэш (SHA-256) токена,
 * не сам токен. По умолчанию живёт 30 дней.
 */
public final class RefreshToken {

    public static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final UUID    id;
    private final UUID    contributorId;
    private final String  tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID contributorId, String tokenHash,
                         Instant expiresAt, Instant createdAt) {
        this.id            = id;
        this.contributorId = contributorId;
        this.tokenHash     = tokenHash;
        this.expiresAt     = expiresAt;
        this.createdAt     = createdAt;
    }

    public static RefreshToken issue(UUID contributorId, String tokenHash, Duration ttl) {
        Objects.requireNonNull(contributorId, "contributorId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Instant now = Instant.now();
        return new RefreshToken(UUID.randomUUID(), contributorId, tokenHash,
            now.plus(ttl == null ? DEFAULT_TTL : ttl), now);
    }

    /** Восстановление из хранилища. */
    public static RefreshToken reconstitute(UUID id, UUID contributorId, String tokenHash,
                                            Instant expiresAt, Instant createdAt) {
        return new RefreshToken(id, contributorId, tokenHash, expiresAt, createdAt);
    }

    public boolean isExpired() { return !Instant.now().isBefore(expiresAt); }

    public UUID    getId()            { return id; }
    public UUID    getContributorId() { return contributorId; }
    public String  getTokenHash()     { return tokenHash; }
    public Instant getExpiresAt()     { return expiresAt; }
    public Instant getCreatedAt()     { return createdAt; }

    @Override public boolean equals(Object o) {
        return o instanceof RefreshToken r && Objects.equals(id, r.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
