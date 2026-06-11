package com.foodscanner.infrastructure.metrics;

import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Слой: infrastructure (unit). Gauge product_extraction_jobs по статусам + zero-fill, очередь. */
class ExtractionMetricsTest {

    @Test
    void publishesGaugePerStatusWithZeroFillAndQueue() {
        ProductExtractionJobRepository repo = mock(ProductExtractionJobRepository.class);
        Map<ExtractionStatus, Long> counts = new EnumMap<>(ExtractionStatus.class);
        counts.put(ExtractionStatus.QUEUED, 4L);
        counts.put(ExtractionStatus.SKIPPED, 7L);
        when(repo.countByStatus()).thenReturn(counts);
        when(repo.countQueued()).thenReturn(4L);
        when(repo.oldestQueuedAt()).thenReturn(Optional.of(Instant.now().minusSeconds(90)));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ExtractionMetrics(repo, registry); // конструктор делает первый refresh

        assertThat(gauge(registry, "QUEUED")).isEqualTo(4.0);
        assertThat(gauge(registry, "SKIPPED")).isEqualTo(7.0);
        assertThat(gauge(registry, "STRUCTURED")).isEqualTo(0.0); // zero-fill
        assertThat(registry.find("product_extraction_jobs").gauges())
            .hasSize(ExtractionStatus.values().length);
        assertThat(registry.get("product_extraction_queue_size").gauge().value()).isEqualTo(4.0);
        assertThat(registry.get("product_extraction_queue_oldest_age_seconds").gauge().value())
            .isGreaterThanOrEqualTo(80.0);
    }

    private double gauge(SimpleMeterRegistry r, String status) {
        return r.get("product_extraction_jobs").tag("status", status).gauge().value();
    }
}
