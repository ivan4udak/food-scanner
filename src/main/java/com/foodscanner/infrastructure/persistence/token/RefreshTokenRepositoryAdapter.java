package com.foodscanner.infrastructure.persistence.token;

import com.foodscanner.domain.model.RefreshToken;
import com.foodscanner.domain.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken t) {
        jpa.save(new RefreshTokenJpaEntity(
            t.getId(), t.getContributorId(), t.getTokenHash(), t.getExpiresAt(), t.getCreatedAt()));
        return t;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) { jpa.deleteById(id); }

    @Override
    @Transactional
    public void deleteByContributorId(UUID contributorId) { jpa.deleteByContributorId(contributorId); }

    @Override
    @Transactional
    public int deleteExpired(Instant now) { return jpa.deleteByExpiresAtBefore(now); }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.reconstitute(
            e.getId(), e.getContributorId(), e.getTokenHash(), e.getExpiresAt(), e.getCreatedAt());
    }
}
