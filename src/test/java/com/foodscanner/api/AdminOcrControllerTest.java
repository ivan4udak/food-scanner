package com.foodscanner.api;

import com.foodscanner.api.controller.AdminOcrController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.application.usecase.ReprocessOcrUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminOcrController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class,
                   com.foodscanner.infrastructure.security.AdminGuardInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminOcrController — reprocess")
class AdminOcrControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ReprocessOcrUseCase reprocessOcr;

    @Test
    @DisplayName("POST /admin/ocr/{id}/reprocess — 200 + новый jobId")
    void reprocessOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        when(reprocessOcr.execute(id)).thenReturn(Optional.of(fresh));
        mockMvc.perform(post("/api/v1/admin/ocr/" + id + "/reprocess"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(fresh.toString()));
    }

    @Test
    @DisplayName("POST reprocess — 404 если задача не найдена")
    void reprocessNotFound() throws Exception {
        when(reprocessOcr.execute(any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/v1/admin/ocr/" + UUID.randomUUID() + "/reprocess"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST reprocess — 409 при неподходящем статусе")
    void reprocessConflict() throws Exception {
        when(reprocessOcr.execute(any())).thenThrow(new IllegalStateException("bad status"));
        mockMvc.perform(post("/api/v1/admin/ocr/" + UUID.randomUUID() + "/reprocess"))
            .andExpect(status().isConflict());
    }
}
