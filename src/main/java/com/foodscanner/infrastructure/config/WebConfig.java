package com.foodscanner.infrastructure.config;

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

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(
                "/api/v1/auth/**",   // login/register/recover/refresh
                "/api/v1/ping"
            );
    }
}
