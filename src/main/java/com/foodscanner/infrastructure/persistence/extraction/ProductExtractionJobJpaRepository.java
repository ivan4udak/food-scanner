package com.foodscanner.infrastructure.persistence.extraction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductExtractionJobJpaRepository extends JpaRepository<ProductExtractionJobJpaEntity, UUID> {

    List<ProductExtractionJobJpaEntity> findByStatusOrderByQueuedAtAsc(short status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductExtractionJobJpaEntity e set e.status=:status, e.attempts=e.attempts+1, e.updatedAt=:now where e.id=:id")
    int markInProgress(@Param("id") UUID id, @Param("status") short status, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ProductExtractionJobJpaEntity e set e.status=:status, e.source=:source,
               e.name=:name, e.brand=:brand, e.manufacturer=:manufacturer, e.composition=:composition,
               e.nutrition=:nutrition, e.confidence=:confidence, e.needsReview=:needsReview,
               e.processedAt=:now, e.updatedAt=:now
        where e.id=:id""")
    int applyResult(@Param("id") UUID id, @Param("status") short status, @Param("source") String source,
                    @Param("name") String name, @Param("brand") String brand,
                    @Param("manufacturer") String manufacturer, @Param("composition") String composition,
                    @Param("nutrition") String nutrition, @Param("confidence") String confidence,
                    @Param("needsReview") Boolean needsReview, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductExtractionJobJpaEntity e set e.status=:status, e.lastError=:error, e.processedAt=:now, e.updatedAt=:now where e.id=:id")
    int markFailed(@Param("id") UUID id, @Param("status") short status, @Param("error") String error, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductExtractionJobJpaEntity e set e.status=:status, e.lastError=:reason, e.processedAt=:now, e.updatedAt=:now where e.id=:id")
    int markSkipped(@Param("id") UUID id, @Param("status") short status, @Param("reason") String reason, @Param("now") Instant now);

    @Query("select e.status, count(e) from ProductExtractionJobJpaEntity e group by e.status")
    List<Object[]> countGroupedByStatus();

    long countByStatus(short status);

    @Query("select min(e.queuedAt) from ProductExtractionJobJpaEntity e where e.status = :status")
    Instant oldestQueuedAt(@Param("status") short status);
}
