package com.foodscanner.api.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Слой: api (DTO).
 * POST /api/v1/admin/users/{id}/role — желаемая роль (USER|ADMIN|SUPER_ADMIN).
 */
public record SetUserRoleRequest(@NotNull String role) {}
