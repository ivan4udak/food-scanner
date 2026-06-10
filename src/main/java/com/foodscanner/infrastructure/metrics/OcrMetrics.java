package com.foodscanner.infrastructure.metrics;

import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
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
 * Публикует gauge `ocr_jobs{status=...,code=...}` — количество OCR-задач по каждому статусу
 * (QUEUED/IN_PROGRESS_READABLE/NEEDS_REVIEW/PHOTO_UNREADABLE/SUCCESS/ERROR).
 * Значения обновляются периодически одним групповым COUNT; сами gauge читают из памяти
 * (дёшево при scrape Prometheus). Видно в /actuator/prometheus → Grafana.
 *
 * Работает независимо от OCR_AMQP_ENABLED — таблица ocr_jobs существует всегда,
 * метрика полезна и для отслеживания накопления QUEUED при выключенном брокере.
 */
@Component
public class OcrMetrics {

    private final OcrJobRepository repo;
    private final Map<OcrStatus, AtomicLong> gauges = new EnumMap<>(OcrStatus.class);
    /** Размер активной очереди (active QUEUED) — backpressure-наблюдаемость. */
    private final AtomicLong queueSize = new AtomicLong(0);
    /** Возраст старейшей активной QUEUED-задачи в секундах (растёт → worker перегружен/недоступен). */
    private final AtomicLong queueOldestAgeSeconds = new AtomicLong(0);

    public OcrMetrics(OcrJobRepository repo, MeterRegistry registry) {
        this.repo = repo;
        for (OcrStatus s : OcrStatus.values()) {
            AtomicLong holder = new AtomicLong(0);
            gauges.put(s, holder);
            Gauge.builder("ocr_jobs", holder, AtomicLong::doubleValue)
                .description("Количество OCR-задач по статусу")
                .tag("status", s.name())
                .tag("code", String.valueOf(s.code()))
                .register(registry);
        }
        Gauge.builder("ocr_queue_size", queueSize, AtomicLong::doubleValue)
            .description("Активные QUEUED OCR-задачи (размер очереди)")
            .register(registry);
        Gauge.builder("ocr_queue_oldest_age_seconds", queueOldestAgeSeconds, AtomicLong::doubleValue)
            .description("Возраст старейшей активной QUEUED OCR-задачи, сек")
            .register(registry);
        refresh();
    }

    /** Обновить значения gauge из БД. Ошибка не валит приложение — метрика вторична. */
    @Scheduled(fixedRateString = "${ocr.metrics.refresh-ms:15000}")
    public void refresh() {
        try {
            repo.countByStatus().forEach((s, c) -> gauges.get(s).set(c));
            queueSize.set(repo.countActiveQueued());
            queueOldestAgeSeconds.set(repo.oldestQueuedCreatedAt()
                .map(t -> Math.max(0, Duration.between(t, Instant.now()).getSeconds()))
                .orElse(0L));
        } catch (Exception ignored) {
            // наблюдаемость не должна ронять приложение
        }
    }
}
