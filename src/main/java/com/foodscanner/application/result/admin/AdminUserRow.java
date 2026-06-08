package com.foodscanner.application.result.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (результат).
 * Строка списка пользователей админки.
 */
public record AdminUserRow(
        UUID id,
        String username,
        String role,
        boolean online,
        Instant lastActivityAt,
        String clientVersion,
        String browser,
        String os,
        String deviceType,
        long totalScans,
        long completedEntries,
        long uploadedPhotos,
        long clientErrors
) {}
