package com.foodscanner.application.result.admin;

import java.util.List;

/**
 * Слой: application (результат).
 * Карточка пользователя: профиль/счётчики + последние сессии и сканы.
 */
public record AdminUserDetail(
        AdminUserRow user,
        List<AdminSessionRow> sessions,
        List<AdminScanRow> recentScans
) {}
