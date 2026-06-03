package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.Contributor;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Repository Interface (Port)
 *
 * Только методы, нужные для четырёх use cases V1.
 * findAll, поиск по partial nickname — откладываем.
 */
public interface ContributorRepository {
    Contributor save(Contributor contributor);
    Optional<Contributor> findById(UUID id);
    boolean existsByNickname(String nickname);
}
