package com.foodscanner.infrastructure.security;

import com.foodscanner.application.port.TokenService;
import com.foodscanner.domain.exception.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Слой: infrastructure
 *
 * Блок 14: проверяет Bearer access-токен на защищённых эндпоинтах.
 * Исключения (login/register/recover/refresh, ping) задаются в WebConfig.
 * Идентификатор пользователя кладётся в request-атрибут CONTRIBUTOR_ATTR —
 * контроллеры берут его как авторитетный (не доверяя телу запроса).
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Имя request-атрибута с id аутентифицированного контрибьютора. */
    public static final String CONTRIBUTOR_ATTR = "authContributorId";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new InvalidTokenException("Требуется авторизация");
        }
        TokenService.AccessClaims claims = tokenService.verifyAccessToken(header.substring(7).trim());
        request.setAttribute(CONTRIBUTOR_ATTR, claims.contributorId());
        return true;
    }
}
