package com.foodscanner.application.result.admin;

import java.time.Instant;

/**
 * Слой: application (результат).
 * Унифицированный элемент сквозной трассировки по correlationId
 * (клиентские логи + серверные события в одной временной линии).
 */
public record TraceItem(
        String source,        // CLIENT | SERVER
        Instant at,
        String level,
        String category,      // категория (client) или useCase (server)
        String event,
        String message,
        String method,
        String path,
        Integer httpStatus,
        Long durationMs
) {}
