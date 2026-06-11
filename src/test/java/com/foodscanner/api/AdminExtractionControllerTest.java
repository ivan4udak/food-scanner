package com.foodscanner.api;

import com.foodscanner.api.controller.AdminExtractionController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.application.usecase.RequeueExtractionUseCase;
import com.foodscanner.application.usecase.SkipExtractionUseCase;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminExtractionController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class,
                   com.foodscanner.infrastructure.security.AdminGuardInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminExtractionController — requeue/skip Contract Tests")
class AdminExtractionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RequeueExtractionUseCase requeue;
    @MockBean SkipExtractionUseCase skip;

    @Test
    @DisplayName("POST requeue → 200 + новый jobId")
    void requeueOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        when(requeue.execute(id)).thenReturn(Optional.of(newId));
        mockMvc.perform(post("/api/v1/admin/extraction/" + id + "/requeue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(newId.toString()));
    }

    @Test
    @DisplayName("POST requeue → 404 если задачи нет")
    void requeueNotFound() throws Exception {
        when(requeue.execute(any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/v1/admin/extraction/" + UUID.randomUUID() + "/requeue"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST requeue → 409 при запрещённом статусе")
    void requeueConflict() throws Exception {
        when(requeue.execute(any())).thenThrow(new IllegalStateException("requeue только для ..."));
        mockMvc.perform(post("/api/v1/admin/extraction/" + UUID.randomUUID() + "/requeue"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST skip → 200")
    void skipOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(skip.execute(id)).thenReturn(Optional.of(id));
        mockMvc.perform(post("/api/v1/admin/extraction/" + id + "/skip"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(id.toString()));
    }

    @Test
    @DisplayName("POST skip → 409 при запрещённом статусе")
    void skipConflict() throws Exception {
        when(skip.execute(any())).thenThrow(new IllegalStateException("skip только для ..."));
        mockMvc.perform(post("/api/v1/admin/extraction/" + UUID.randomUUID() + "/skip"))
            .andExpect(status().isConflict());
    }
}
