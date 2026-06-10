package com.foodscanner.api;

import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.api.controller.MeScansController;
import com.foodscanner.application.result.me.MeScanDetail;
import com.foodscanner.application.result.me.MeScanRow;
import com.foodscanner.application.usecase.MyScansUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MeScansController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class,
                   com.foodscanner.infrastructure.security.AdminGuardInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("MeScansController — Contract Tests")
class MeScansControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MyScansUseCase myScans;

    private static final UUID ME = UUID.randomUUID();

    @Test
    @DisplayName("GET /me/scans — список своих сканов")
    void list() throws Exception {
        when(myScans.list(eq(ME))).thenReturn(List.of(
            new MeScanRow("460", "COMPLETED", UUID.randomUUID(), Instant.now(), Instant.now(), 4, null)));
        mockMvc.perform(get("/api/v1/me/scans").requestAttr("authContributorId", ME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].barcode").value("460"))
            .andExpect(jsonPath("$[0].scanStatus").value("COMPLETED"))
            .andExpect(jsonPath("$[0].photoCount").value(4));
    }

    @Test
    @DisplayName("GET /me/scans/{barcode} — детали с URL фото")
    void detail() throws Exception {
        when(myScans.detail(eq(ME), eq("460"))).thenReturn(Optional.of(new MeScanDetail(
            "460", UUID.randomUUID(), Instant.now(), Instant.now(),
            List.of(new MeScanDetail.Photo(UUID.randomUUID(), "FRONT", "photos/h.jpg",
                "/api/v1/photos/photos/h.jpg?size=thumb", "/api/v1/photos/photos/h.jpg?size=full", null)),
            List.of(), null)));
        mockMvc.perform(get("/api/v1/me/scans/460").requestAttr("authContributorId", ME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.barcode").value("460"))
            .andExpect(jsonPath("$.photos[0].thumbUrl").value("/api/v1/photos/photos/h.jpg?size=thumb"));
    }

    @Test
    @DisplayName("GET /me/scans/{barcode} — 404 если не найдено/не своё")
    void notFound() throws Exception {
        when(myScans.detail(any(), any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/me/scans/000").requestAttr("authContributorId", ME))
            .andExpect(status().isNotFound());
    }
}
