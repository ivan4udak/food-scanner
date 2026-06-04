package com.foodscanner.infrastructure.persistence.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/** Тонкий репозиторий для подсчёта ссылок на объект (дедупликация/очистка). */
public interface CatalogEntryPhotoJpaRepository extends JpaRepository<CatalogEntryPhotoJpaEntity, UUID> {
    long countByStorageKey(String storageKey);
}
