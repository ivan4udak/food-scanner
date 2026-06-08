package com.foodscanner.application.result.admin;

/**
 * Слой: application (результат).
 * Сводка для админ-дашборда (операционная панель).
 */
public record AdminDashboard(
        long usersTotal,
        long onlineNow,
        long activeToday,
        long activeWeek,
        long scansToday,
        long scansWeek,
        long entriesToday,
        long entriesWeek,
        long photosToday,
        long clientErrorsToday,
        long serverErrorsToday
) {}
