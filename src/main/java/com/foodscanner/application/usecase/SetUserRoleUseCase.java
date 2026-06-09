package com.foodscanner.application.usecase;

import com.foodscanner.domain.model.ContributorRole;

import java.util.UUID;

/**
 * Слой: application (use case).
 * Смена роли пользователя. Доступно только SUPER_ADMIN (проверяется в сервисе).
 */
public interface SetUserRoleUseCase {
    /** @return назначенная роль. */
    ContributorRole execute(ContributorRole callerRole, UUID targetId, ContributorRole newRole);
}
