package com.foodscanner.application.service;

import org.springframework.stereotype.Component;

/**
 * Слой: application.
 *
 * Политика исключения heartbeat-шума: успешные GET /ping и /health не должны
 * засорять прикладные таблицы (client_logs, server_events). Сохраняем их только
 * при ошибке (4xx/5xx) либо при отсутствии http-статуса (сетевая/таймаут-ошибка
 * с уровнем WARN/ERROR — такие проблемные события пропускаем дальше).
 */
@Component
public class HealthNoisePolicy {

    /**
     * @return true, если запись о вызове ping/health является шумом и не должна сохраняться.
     */
    public boolean isNoise(String apiPath, Integer httpStatus, String level) {
        if (!isHealthPath(apiPath)) {
            return false;
        }
        // Проблемное событие (явный 4xx/5xx) — сохраняем.
        if (httpStatus != null && httpStatus >= 400) {
            return false;
        }
        // Нет статуса, но это явная ошибка/предупреждение (timeout, network, offline) — сохраняем.
        if (httpStatus == null && isProblemLevel(level)) {
            return false;
        }
        // Всё остальное по ping/health (2xx/3xx или штатный INFO без статуса) — шум.
        return true;
    }

    private static boolean isHealthPath(String apiPath) {
        if (apiPath == null) return false;
        String p = apiPath.toLowerCase();
        return p.endsWith("/api/v1/ping") || p.endsWith("/api/v1/health")
            || p.endsWith("/ping") || p.endsWith("/health");
    }

    private static boolean isProblemLevel(String level) {
        return "WARN".equalsIgnoreCase(level) || "ERROR".equalsIgnoreCase(level);
    }
}
