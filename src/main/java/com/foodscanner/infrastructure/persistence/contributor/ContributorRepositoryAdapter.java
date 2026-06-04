package com.foodscanner.infrastructure.persistence.contributor;

import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: infrastructure
 * Адаптер между JPA и доменным репозиторием. Маппинг domain ↔ JPA явный.
 */
@Repository
public class ContributorRepositoryAdapter implements ContributorRepository {

    private final ContributorJpaRepository jpa;

    public ContributorRepositoryAdapter(ContributorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Contributor save(Contributor contributor) {
        jpa.save(toJpa(contributor));
        return contributor;
    }

    @Override
    public Optional<Contributor> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpa.existsByNickname(nickname);
    }

    @Override
    public Optional<Contributor> findByUsername(String username) {
        return jpa.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<Contributor> findByNickname(String nickname) {
        return jpa.findByNickname(nickname).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    // ──────────────────────────────────────────────
    private ContributorJpaEntity toJpa(Contributor c) {
        return new ContributorJpaEntity(
            c.getId(), c.getNickname(), c.getUsername(), c.getPasswordHash(),
            c.getFailedLoginAttempts(), c.getLockedUntil(), c.getResetPasswordUntil(),
            c.getCompletedCatalogCount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private Contributor toDomain(ContributorJpaEntity e) {
        return Contributor.reconstitute(
            e.getId(), e.getNickname(), e.getUsername(), e.getPasswordHash(),
            e.getFailedLoginAttempts(), e.getLockedUntil(), e.getResetPasswordUntil(),
            e.getCompletedCatalogCount(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
