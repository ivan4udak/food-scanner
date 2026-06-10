package com.foodscanner.infrastructure.persistence.ocr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    // ── Результат OCR (обновление, не трогая lifecycle-поля) ──────────────
    @Modifying
    @Query("""
        update OcrJobJpaEntity e set e.status=:status, e.attempts=:attempts, e.rawText=:rawText,
               e.parsedIngredients=:ingredients, e.parsedNutrition=:nutrition, e.confidence=:confidence,
               e.errorCode=:errorCode, e.errorMessage=:errorMessage, e.updatedAt=:updatedAt
        where e.id=:id""")
    int updateResult(@Param("id") UUID id, @Param("status") short status, @Param("attempts") int attempts,
                     @Param("rawText") String rawText, @Param("ingredients") String ingredients,
                     @Param("nutrition") String nutrition, @Param("confidence") Double confidence,
                     @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage,
                     @Param("updatedAt") Instant updatedAt);

    // ── Lifecycle ─────────────────────────────────────────────────────────
    /** Пометить прошлые активные задачи (draft, photo_type) как замещённые новой. */
    @Modifying
    @Query("""
        update OcrJobJpaEntity e set e.active=false, e.supersededAt=:now, e.supersededBy=:newId
        where e.draftId=:draftId and e.photoType=:photoType and e.id<>:newId and e.active=true""")
    int supersedePrevious(@Param("draftId") UUID draftId, @Param("photoType") String photoType,
                          @Param("newId") UUID newId, @Param("now") Instant now);

    /** Orphan: активные задачи без catalog_entry и с исчезнувшим черновиком. */
    @Modifying
    @Query(nativeQuery = true, value = """
        UPDATE food_catalog.ocr_jobs SET orphaned=true, orphaned_at=:now, active=false
        WHERE active=true AND orphaned=false AND catalog_entry_id IS NULL AND draft_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM food_catalog.catalog_drafts d WHERE d.id = draft_id)""")
    int markOrphans(@Param("now") Instant now);

    // ── Публикация / backpressure ──────────────────────────────────────────
    @Modifying
    @Query("update OcrJobJpaEntity e set e.publishedAt=:now, e.publishAttempts=e.publishAttempts+1, e.lastPublishError=null where e.id=:id")
    int markPublished(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("update OcrJobJpaEntity e set e.publishAttempts=e.publishAttempts+1, e.lastPublishError=:error where e.id=:id")
    int recordPublishFailure(@Param("id") UUID id, @Param("error") String error);

    /** Активные QUEUED, ещё не опубликованные — кандидаты на republish. */
    List<OcrJobJpaEntity> findTop50ByActiveTrueAndStatusAndPublishedAtIsNullOrderByCreatedAtAsc(short status);

    // ── Метрики очереди ─────────────────────────────────────────────────────
    long countByActiveTrueAndStatus(short status);

    Optional<OcrJobJpaEntity> findTopByActiveTrueAndStatusOrderByCreatedAtAsc(short status);
}
