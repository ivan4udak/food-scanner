package com.foodscanner.application.result;

/**
 * Слой: application (результат чтения).
 * Сырая строка рейтинга из read-порта (без rank/score — их присваивает сервис).
 */
public record LeaderboardRow(String username, long completedEntries, long scans, long uploadedPhotos) {}
