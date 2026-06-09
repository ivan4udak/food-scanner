package com.foodscanner.api;

import com.foodscanner.api.controller.AdminReadController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.application.result.admin.*;
import com.foodscanner.application.usecase.AdminReadUseCase;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminReadController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class,
                   com.foodscanner.infrastructure.security.AdminGuardInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminReadController — Contract Tests")
class AdminReadControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AdminReadUseCase admin;

    @Test
    @DisplayName("GET /admin/dashboard")
    void dashboard() throws Exception {
        when(admin.dashboard()).thenReturn(new AdminDashboard(180, 5, 40, 90, 12, 80, 3, 20, 30, 2, 1));
        mockMvc.perform(get("/api/v1/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usersTotal").value(180))
            .andExpect(jsonPath("$.onlineNow").value(5))
            .andExpect(jsonPath("$.clientErrorsToday").value(2));
    }

    @Test
    @DisplayName("GET /admin/users")
    void users() throws Exception {
        when(admin.users(any(), anyInt(), anyInt())).thenReturn(List.of(
            new AdminUserRow(UUID.randomUUID(), "ivan", "ADMIN", true, Instant.now(),
                "1.7.0", "Safari", "iOS", "mobile", 10, 4, 20, 1)));
        mockMvc.perform(get("/api/v1/admin/users").param("sort", "completedEntries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("ivan"))
            .andExpect(jsonPath("$[0].online").value(true))
            .andExpect(jsonPath("$[0].completedEntries").value(4));
    }

    @Test
    @DisplayName("GET /admin/users/{id} — 404 если нет")
    void userNotFound() throws Exception {
        when(admin.userDetail(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/admin/users/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /admin/catalog/{barcode} — 404 если нет")
    void catalogNotFound() throws Exception {
        when(admin.catalogDetail(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/admin/catalog/000"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /admin/users/by-username/{username} — 200 / 404")
    void userByName() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUserRow row = new AdminUserRow(id, "ivan", "ADMIN", true, Instant.now(),
            "1.8.0", "Safari", "iOS", "mobile", 3, 1, 5, 0);
        when(admin.userDetailByUsername(eq("ivan")))
            .thenReturn(Optional.of(new AdminUserDetail(row, List.of(), List.of())));
        mockMvc.perform(get("/api/v1/admin/users/by-username/ivan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value("ivan"));

        when(admin.userDetailByUsername(eq("ghost"))).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/admin/users/by-username/ghost"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /admin/trace/{correlationId}")
    void trace() throws Exception {
        UUID corr = UUID.randomUUID();
        when(admin.trace(eq(corr))).thenReturn(List.of(
            new TraceItem("SERVER", Instant.now(), "INFO", "ScanBarcode", "SCAN_COMPLETED",
                "ok", "POST", "/api/v1/scan", 200, 60L)));
        mockMvc.perform(get("/api/v1/admin/trace/" + corr))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].source").value("SERVER"))
            .andExpect(jsonPath("$[0].event").value("SCAN_COMPLETED"));
    }
}
