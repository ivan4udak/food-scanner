package com.foodscanner.domain.policy;

import java.time.LocalTime;

/**
 * Слой: domain (policy, чистая логика). Окно обработки (ночной батч), включая переход через полночь.
 * [start, end): 00:00–06:00 — обычное; 23:00–06:00 — через полночь.
 */
public final class ProcessingWindow {

    private final LocalTime start;
    private final LocalTime end;

    public ProcessingWindow(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public static ProcessingWindow of(String start, String end) {
        return new ProcessingWindow(LocalTime.parse(start), LocalTime.parse(end));
    }

    public boolean contains(LocalTime now) {
        if (start.equals(end)) return true; // 24/7
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);     // обычное окно
        }
        return !now.isBefore(start) || now.isBefore(end);         // через полночь
    }
}
