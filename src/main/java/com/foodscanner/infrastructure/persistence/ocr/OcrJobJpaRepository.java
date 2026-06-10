package com.foodscanner.infrastructure.persistence.ocr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OcrJobJpaRepository extends JpaRepository<OcrJobJpaEntity, UUID> {
    List<OcrJobJpaEntity> findByDraftId(UUID draftId);

    /** Количество задач по статусу (короткий код → count). Только непустые группы. */
    @Query("select e.status as status, count(e) as cnt from OcrJobJpaEntity e group by e.status")
    List<StatusCount> countGroupedByStatus();

    /** Проекция результата группировки по статусу. */
    interface StatusCount {
        short getStatus();
        long getCnt();
    }
}
