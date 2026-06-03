package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест ContributorRepositoryAdapter.
 * Требует запущенного Docker. При отсутствии Docker — тест пропускается.
 *
 * Запуск: mvn test -Dtest=ContributorRepositoryAdapterIT
 */
@DisplayName("ContributorRepository — интеграционный тест")
class ContributorRepositoryAdapterIT extends AbstractRepositoryIT {

    @Autowired
    ContributorRepository repository;

    @Test
    @DisplayName("Сохраняет и находит контрибьютора по id")
    void shouldSaveAndFindById() {
        Contributor contributor = Contributor.create("alice_it");
        repository.save(contributor);

        Optional<Contributor> found = repository.findById(contributor.getId());

        assertTrue(found.isPresent());
        assertEquals("alice_it", found.get().getNickname());
        assertEquals(0, found.get().getCompletedCatalogCount());
    }

    @Test
    @DisplayName("existsByNickname возвращает true для существующего")
    void shouldReturnTrueForExistingNickname() {
        repository.save(Contributor.create("bob_it"));
        assertTrue(repository.existsByNickname("bob_it"));
    }

    @Test
    @DisplayName("existsByNickname возвращает false для несуществующего")
    void shouldReturnFalseForMissingNickname() {
        assertFalse(repository.existsByNickname("nobody_xyz"));
    }

    @Test
    @DisplayName("Сохраняет обновлённый completedCatalogCount")
    void shouldSaveIncrementedCount() {
        Contributor contributor = Contributor.create("charlie_it");
        contributor.incrementCompletedCatalogs();
        contributor.incrementCompletedCatalogs();
        repository.save(contributor);

        Contributor found = repository.findById(contributor.getId()).orElseThrow();
        assertEquals(2, found.getCompletedCatalogCount());
    }
}
