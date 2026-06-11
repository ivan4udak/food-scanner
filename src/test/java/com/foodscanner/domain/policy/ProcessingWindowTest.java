package com.foodscanner.domain.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Слой: domain (unit). Окно обработки, включая переход через полночь. */
class ProcessingWindowTest {

    @Test
    void normalWindow() {
        ProcessingWindow w = ProcessingWindow.of("00:00", "06:00");
        assertThat(w.contains(LocalTime.of(2, 0))).isTrue();
        assertThat(w.contains(LocalTime.of(0, 0))).isTrue();
        assertThat(w.contains(LocalTime.of(6, 0))).isFalse();   // конец исключается
        assertThat(w.contains(LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void overnightWindow() {
        ProcessingWindow w = ProcessingWindow.of("23:00", "06:00");
        assertThat(w.contains(LocalTime.of(23, 30))).isTrue();
        assertThat(w.contains(LocalTime.of(2, 0))).isTrue();
        assertThat(w.contains(LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void allDayWhenStartEqualsEnd() {
        ProcessingWindow w = ProcessingWindow.of("00:00", "00:00");
        assertThat(w.contains(LocalTime.of(3, 0))).isTrue();
        assertThat(w.contains(LocalTime.of(15, 0))).isTrue();
    }
}
