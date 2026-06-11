package com.foodscanner.infrastructure.metrics;

import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Слой: infrastructure (наблюдаемость).
 *
 * Публикует gauge `product_extraction_jobs{status=...,code=...}` — количество задач извлечения
 * по каждому статусу (QUEUED/IN_PROGRESS/STRUCTURED/NEEDS_REVIEW/FAILED/SKIPPED), а также
 * размер очереди и возраст старейшей QUEUED-задачи. Значения обновляются периодически одним
 * групповым COUNT; gauge читают из памяти (дёшево при scrape). Видно в /actuator/prometheus → Grafana.
 *
 * Извлечение обрабатывается ночным окном — рост QUEUED днём ожидаем; метрика возраста очереди
 * помогает заметить, что ночной воркер не отрабатывает (очередь не рассасывается).
 */
@Component
public class ExtractionMetrics {

    private final ProductExtractionJobRepository repo;
    private final Map<ExtractionStatus, AtomicLong> gauges = new EnumMap<>(ExtractionStatus.class);
    private final AtomicLong queueSize = new AtomicLong(0);
    private final AtomicLong queueOldestAgeSeconds = new AtomicLong(0);

    public ExtractionMetrics(ProductExtractionJobRepository repo, MeterRegistry registry) {
        this.repo = repo;
        for (ExtractionStatus s : ExtractionStatus.values()) {
            AtomicLong holder = new AtomicLong(0);
            gauges.put(s, holder);
            Gauge.builder("product_extraction_jobs", holder, AtomicLong::doubleValue)
                .description("Количество задач извлечения по статусу")
                .tag("status", s.name())
                .tag("code", String.valueOf(s.code()))
                .register(registry);
        }
        Gauge.builder("product_extraction_queue_size", queueSize, AtomicLong::doubleValue)
            .description("QUEUED задачи извлечения (размер очереди)")
            .register(registry);
        Gauge.builder("product_extraction_queue_oldest_age_seconds", queueOldestAgeSeconds, AtomicLong::doubleValue)
            .description("Возраст старейшей QUEUED задачи извлечения, сек")
            .register(registry);
        refresh();
    }

    /** Обновить значения gauge из БД. Ошибка не валит приложение — метрика вторична. */
    @Scheduled(fixedRateString = "${product.extraction.metrics.refresh-ms:15000}")
    public void refresh() {
        try {
            Map<ExtractionStatus, Long> counts = repo.countByStatus();
            for (ExtractionStatus s : ExtractionStatus.values()) {
                gauges.get(s).set(counts.getOrDefault(s, 0L));   // zero-fill
            }
            queueSize.set(repo.countQueued());
            queueOldestAgeSeconds.set(repo.oldestQueuedAt()
                .map(t -> Math.max(0, Duration.between(t, Instant.now()).getSeconds()))
                .orElse(0L));
        } catch (Exception ignored) {
            // наблюдаемость не должна ронять приложение
        }
    }
}
