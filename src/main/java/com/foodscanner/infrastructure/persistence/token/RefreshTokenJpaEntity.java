package com.foodscanner.infrastructure.persistence.token;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "food_catalog", name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenJpaEntity() {}

    public RefreshTokenJpaEntity(UUID id, UUID contributorId, String tokenHash,
                                 Instant expiresAt, Instant createdAt) {
        this.id            = id;
        this.contributorId = contributorId;
        this.tokenHash     = tokenHash;
        this.expiresAt     = expiresAt;
        this.createdAt     = createdAt;
    }

    public UUID    getId()            { return id; }
    public UUID    getContributorId() { return contributorId; }
    public String  getTokenHash()     { return tokenHash; }
    public Instant getExpiresAt()     { return expiresAt; }
    public Instant getCreatedAt()     { return createdAt; }
}
