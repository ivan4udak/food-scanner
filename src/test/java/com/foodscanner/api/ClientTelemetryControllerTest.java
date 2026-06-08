package com.foodscanner.api;

import com.foodscanner.api.controller.ClientTelemetryController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.api.mapper.TelemetryApiMapper;
import com.foodscanner.application.result.IngestClientLogsResult;
import com.foodscanner.application.usecase.IngestClientLogsUseCase;
import com.foodscanner.application.usecase.RecordClientActivityUseCase;
import com.foodscanner.application.usecase.RecordClientSessionUseCase;
import com.foodscanner.application.usecase.RecordServerEventUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientTelemetryController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class}))
@Import({TelemetryApiMapper.class, GlobalExceptionHandler.class})
@DisplayName("ClientTelemetryController — Contract Tests")
class ClientTelemetryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean IngestClientLogsUseCase ingestLogs;
    @MockBean RecordClientSessionUseCase recordSession;
    @MockBean RecordClientActivityUseCase recordActivity;
    @MockBean RecordServerEventUseCase recordServerEvent;

    private static final UUID CONTRIBUTOR = UUID.randomUUID();

    @Test
    @DisplayName("POST /client-logs/batch — 200, accepted из use case")
    void batch() throws Exception {
        when(ingestLogs.execute(any())).thenReturn(new IngestClientLogsResult(2));

        mockMvc.perform(post("/api/v1/client-logs/batch")
                .requestAttr("authContributorId", CONTRIBUTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sessionId": "%s",
                      "clientVersion": "1.7.0",
                      "pwaVersion": "1.7.0",
                      "logs": [
                        {"id":"c1","timestamp":"2026-06-08T10:00:00Z","level":"INFO","category":"SCAN",
                         "event":"SCAN_RESULT","message":"Scan result NEW","barcode":"460"}
                      ]
                    }""".formatted(UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.accepted").value(2));

        verify(ingestLogs).execute(any());
        verify(recordServerEvent).execute(any());
    }

    @Test
    @DisplayName("POST /client/session — 200 OK")
    void session() throws Exception {
        mockMvc.perform(post("/api/v1/client/session")
                .requestAttr("authContributorId", CONTRIBUTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sessionId":"%s","browser":"Safari","os":"iOS","deviceType":"mobile",
                     "standalone":true,"screenWidth":390}""".formatted(UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"));

        verify(recordSession).execute(any());
    }

    @Test
    @DisplayName("POST /client/activity — 200 OK, server event не пишется")
    void activity() throws Exception {
        mockMvc.perform(post("/api/v1/client/activity")
                .requestAttr("authContributorId", CONTRIBUTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sessionId":"%s","screen":"ScannerPage","online":true,
                     "timestamp":"2026-06-08T10:00:00Z"}""".formatted(UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"));

        verify(recordActivity).execute(any());
        verify(recordServerEvent, never()).execute(any());
    }

    @Test
    @DisplayName("POST /client/session без sessionId — 400")
    void sessionValidation() throws Exception {
        mockMvc.perform(post("/api/v1/client/session")
                .requestAttr("authContributorId", CONTRIBUTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"browser\":\"Safari\"}"))
            .andExpect(status().isBadRequest());
    }
}
