package com.foodscanner.application.result;

/**
 * Слой: application (результат).
 * @param accepted число фактически сохранённых записей (после фильтрации шума).
 */
public record IngestClientLogsResult(int accepted) {}
