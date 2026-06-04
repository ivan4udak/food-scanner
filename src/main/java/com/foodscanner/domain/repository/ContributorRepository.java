package com.foodscanner.domain.repository;

import com.foodscanner.domain.model.Contributor;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Repository Interface (Port)
 */
public interface ContributorRepository {
    Contributor save(Contributor contributor);
    Optional<Contributor> findById(UUID id);
    boolean existsByNickname(String nickname);

    // vNext: аутентификация
    Optional<Contributor> findByUsername(String username);
    Optional<Contributor> findByNickname(String nickname);
    void deleteById(UUID id);
}
