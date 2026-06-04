package com.foodscanner.infrastructure.persistence.draft;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/** Тонкий репозиторий для подсчёта ссылок на объект (дедупликация/очистка). */
public interface DraftPhotoJpaRepository extends JpaRepository<DraftPhotoJpaEntity, UUID> {
    long countByStorageKey(String storageKey);
}
