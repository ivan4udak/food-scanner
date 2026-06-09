package com.foodscanner.infrastructure.security;

import com.foodscanner.domain.exception.AccessDeniedException;
import com.foodscanner.domain.model.ContributorRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Слой: infrastructure.
 *
 * Гард админ-панели: разрешает /api/v1/admin/** только ролям ADMIN/SUPER_ADMIN.
 * Выполняется ПОСЛЕ AuthInterceptor (тот уже положил роль в request-атрибут).
 */
@Component
public class AdminGuardInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object role = request.getAttribute(AuthInterceptor.ROLE_ATTR);
        if (role == null || !ContributorRole.parse(role.toString()).isAdmin()) {
            throw new AccessDeniedException("Недостаточно прав");
        }
        return true;
    }
}
