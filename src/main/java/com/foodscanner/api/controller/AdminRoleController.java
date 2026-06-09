package com.foodscanner.api.controller;

import com.foodscanner.api.dto.admin.SetUserRoleRequest;
import com.foodscanner.application.usecase.SetUserRoleUseCase;
import com.foodscanner.domain.model.ContributorRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Слой: api.
 * Смена роли пользователя (только SUPER_ADMIN). Под гардом /api/v1/admin/**;
 * дополнительная проверка SUPER_ADMIN — в сервисе (ADMIN получит 403).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminRoleController {

    /** Роль вызывающего кладёт AuthInterceptor (ROLE_ATTR = "authRole"). */
    private static final String AUTH_ROLE = "authRole";

    private final SetUserRoleUseCase setUserRole;

    public AdminRoleController(SetUserRoleUseCase setUserRole) {
        this.setUserRole = setUserRole;
    }

    @PostMapping("/users/{id}/role")
    public Map<String, String> setRole(
            @PathVariable UUID id,
            @Valid @RequestBody SetUserRoleRequest body,
            @RequestAttribute(value = AUTH_ROLE, required = false) String callerRole) {
        ContributorRole role = setUserRole.execute(
            ContributorRole.parse(callerRole), id, ContributorRole.parse(body.role()));
        return Map.of("role", role.name());
    }
}
