package com.foodscanner.infrastructure.metrics;

import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Слой: infrastructure (unit). Gauge ocr_jobs по статусам + zero-fill. */
class OcrMetricsTest {

    @Test
    void publishesGaugePerStatusWithZeroFill() {
        OcrJobRepository repo = mock(OcrJobRepository.class);
        Map<OcrStatus, Long> counts = new EnumMap<>(OcrStatus.class);
        for (OcrStatus s : OcrStatus.values()) counts.put(s, 0L);
        counts.put(OcrStatus.QUEUED, 3L);
        counts.put(OcrStatus.NEEDS_REVIEW, 5L);
        when(repo.countByStatus()).thenReturn(counts);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OcrMetrics(repo, registry); // конструктор делает первый refresh

        assertThat(gauge(registry, "QUEUED")).isEqualTo(3.0);
        assertThat(gauge(registry, "NEEDS_REVIEW")).isEqualTo(5.0);
        assertThat(gauge(registry, "SUCCESS")).isEqualTo(0.0); // zero-fill
        assertThat(registry.find("ocr_jobs").gauges()).hasSize(OcrStatus.values().length);
    }

    private double gauge(SimpleMeterRegistry r, String status) {
        return r.get("ocr_jobs").tag("status", status).gauge().value();
    }
}
