package com.foodscanner.api;

import com.foodscanner.api.controller.HealthController;
import com.foodscanner.application.port.PhotoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Слой: api
 * Тип: WebMvcTest (Contract Test)
 *
 * Контракт GET /api/v1/health: статусы backend/storage и общий status.
 */
@WebMvcTest(controllers = HealthController.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class}))
@DisplayName("HealthController — Contract Tests")
class HealthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PhotoStorage photoStorage;

    @Test
    @DisplayName("storage доступен → 200 {status:OK, backend:UP, storage:UP}")
    void shouldReturnOkWhenStorageUp() throws Exception {
        when(photoStorage.isAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.backend").value("UP"))
            .andExpect(jsonPath("$.storage").value("UP"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("storage недоступен → 200 {status:DEGRADED, backend:UP, storage:DOWN}")
    void shouldReturnDegradedWhenStorageDown() throws Exception {
        when(photoStorage.isAvailable()).thenReturn(false);

        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DEGRADED"))
            .andExpect(jsonPath("$.backend").value("UP"))
            .andExpect(jsonPath("$.storage").value("DOWN"));
    }
}
