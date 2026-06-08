package com.foodscanner.infrastructure.config;

import com.foodscanner.infrastructure.security.AdminGuardInterceptor;
import com.foodscanner.infrastructure.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Слой: infrastructure
 * Регистрирует AuthInterceptor: Bearer обязателен на всём /api/v1/**,
 * кроме auth/** (login/register/recover/refresh) и ping.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminGuardInterceptor adminGuardInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, AdminGuardInterceptor adminGuardInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminGuardInterceptor = adminGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(
                "/api/v1/auth/**",     // login/register/recover/refresh
                "/api/v1/ping",
                "/api/v1/health",      // диагностика (экран «О приложении»)
                "/api/v1/public/**"    // публичная статистика и рейтинг (без авторизации)
            );
        // Гард админ-панели — после AuthInterceptor (роль уже в request-атрибуте).
        registry.addInterceptor(adminGuardInterceptor)
            .addPathPatterns("/api/v1/admin/**");
    }
}
