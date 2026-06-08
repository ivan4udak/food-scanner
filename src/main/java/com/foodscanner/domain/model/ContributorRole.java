package com.foodscanner.domain.model;

/**
 * Слой: domain.
 * Роль участника. USER — обычный; ADMIN/SUPER_ADMIN — доступ к админ-панели.
 */
public enum ContributorRole {
    USER,
    ADMIN,
    SUPER_ADMIN;

    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    /** Толерантный разбор строки из БД/токена (по умолчанию USER). */
    public static ContributorRole parse(String raw) {
        if (raw == null) return USER;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return USER;
        }
    }
}
