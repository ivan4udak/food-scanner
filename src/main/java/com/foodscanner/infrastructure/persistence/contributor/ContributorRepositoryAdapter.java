package com.foodscanner.infrastructure.persistence.contributor;

import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: infrastructure
 *
 * Адаптер между JPA и доменным репозиторием.
 * Маппинг domain ↔ JPA явный — нет магии, нет MapStruct в MVP.
 */
@Repository
public class ContributorRepositoryAdapter implements ContributorRepository {

    private final ContributorJpaRepository jpa;

    public ContributorRepositoryAdapter(ContributorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Contributor save(Contributor contributor) {
        ContributorJpaEntity entity = toJpa(contributor);
        jpa.save(entity);
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

    // ──────────────────────────────────────────────
    private ContributorJpaEntity toJpa(Contributor c) {
        return new ContributorJpaEntity(
            c.getId(), c.getNickname(), c.getCompletedCatalogCount(),
            c.getCreatedAt(), c.getUpdatedAt());
    }

    private Contributor toDomain(ContributorJpaEntity e) {
        return Contributor.reconstitute(
            e.getId(), e.getNickname(), e.getCompletedCatalogCount(),
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
