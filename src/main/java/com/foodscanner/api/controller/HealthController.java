package com.foodscanner.api.controller;

import com.foodscanner.api.dto.HealthResponse;
import com.foodscanner.application.port.PhotoStorage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Слой: api
 * GET /api/v1/health — диагностика: состояние backend + хранилища (MinIO).
 * Публичный (как /ping): нужен экрану «О приложении» независимо от токена.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final PhotoStorage photoStorage;

    public HealthController(PhotoStorage photoStorage) {
        this.photoStorage = photoStorage;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.from(photoStorage.isAvailable());
    }
}
