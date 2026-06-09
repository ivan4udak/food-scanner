package com.foodscanner.application.result.admin;

import java.util.List;

/**
 * Слой: application (результат).
 * Сводка ошибок за сегодня: клиентские (WARN/ERROR) и серверные события.
 */
public record AdminErrors(List<AdminClientLog> client, List<AdminServerEventRow> server) {}
