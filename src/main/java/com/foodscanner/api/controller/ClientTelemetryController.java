package com.foodscanner.api.controller;

import com.foodscanner.api.dto.telemetry.ClientActivityRequest;
import com.foodscanner.api.dto.telemetry.ClientLogBatchRequest;
import com.foodscanner.api.dto.telemetry.ClientLogBatchResponse;
import com.foodscanner.api.dto.telemetry.ClientSessionRequest;
import com.foodscanner.api.dto.telemetry.TelemetryStatusResponse;
import com.foodscanner.api.filter.CorrelationIdFilter;
import com.foodscanner.application.command.RecordServerEventCommand;
import com.foodscanner.application.result.IngestClientLogsResult;
import com.foodscanner.application.usecase.IngestClientLogsUseCase;
import com.foodscanner.application.usecase.RecordClientActivityUseCase;
import com.foodscanner.application.usecase.RecordClientSessionUseCase;
import com.foodscanner.application.usecase.RecordServerEventUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: api.
 *
 * Приём клиентской телеметрии (требует Bearer):
 *   POST /api/v1/client-logs/batch — партия клиентских логов
 *   POST /api/v1/client/session    — снимок сессии (upsert)
 *   POST /api/v1/client/activity   — heartbeat активности
 *
 * contributorId берётся из токена (AuthInterceptor), не из тела.
 * Значимые приёмы фиксируются как server_events (с correlationId из фильтра).
 */
@RestController
@RequestMapping("/api/v1")
public class ClientTelemetryController {

    private static final String AUTH_CONTRIBUTOR = "authContributorId";

    private final IngestClientLogsUseCase ingestLogs;
    private final RecordClientSessionUseCase recordSession;
    private final RecordClientActivityUseCase recordActivity;
    private final RecordServerEventUseCase recordServerEvent;
    private final com.foodscanner.api.mapper.TelemetryApiMapper mapper;

    public ClientTelemetryController(IngestClientLogsUseCase ingestLogs,
                                     RecordClientSessionUseCase recordSession,
                                     RecordClientActivityUseCase recordActivity,
                                     RecordServerEventUseCase recordServerEvent,
                                     com.foodscanner.api.mapper.TelemetryApiMapper mapper) {
        this.ingestLogs = ingestLogs;
        this.recordSession = recordSession;
        this.recordActivity = recordActivity;
        this.recordServerEvent = recordServerEvent;
        this.mapper = mapper;
    }

    @PostMapping("/client-logs/batch")
    public ResponseEntity<ClientLogBatchResponse> ingestBatch(
            @Valid @RequestBody ClientLogBatchRequest request,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId,
            HttpServletRequest http) {

        IngestClientLogsResult result = ingestLogs.execute(
            mapper.toCommand(request, contributorId, Instant.now()));

        recordServerEvent.execute(serverEvent("CLIENT_LOGS_BATCH_RECEIVED", contributorId,
            request.sessionId(), "POST", "/api/v1/client-logs/batch", http)
            .build());

        return ResponseEntity.ok(ClientLogBatchResponse.ok(result.accepted()));
    }

    @PostMapping("/client/session")
    public ResponseEntity<TelemetryStatusResponse> session(
            @Valid @RequestBody ClientSessionRequest request,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId,
            HttpServletRequest http) {

        recordSession.execute(mapper.toCommand(request, contributorId, Instant.now()));

        recordServerEvent.execute(serverEvent("CLIENT_SESSION_STARTED", contributorId,
            request.sessionId(), "POST", "/api/v1/client/session", http)
            .build());

        return ResponseEntity.ok(TelemetryStatusResponse.OK);
    }

    @PostMapping("/client/activity")
    public ResponseEntity<TelemetryStatusResponse> activity(
            @Valid @RequestBody ClientActivityRequest request,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {

        // Активность — каждые 5с, как server_event не пишем (шум). Запись в client_activity достаточно.
        recordActivity.execute(mapper.toCommand(request, contributorId));
        return ResponseEntity.ok(TelemetryStatusResponse.OK);
    }

    // ── helpers ──────────────────────────────────────────────
    private RecordServerEventCommand.Builder serverEvent(
            String event, UUID contributorId, UUID sessionId, String method, String path,
            HttpServletRequest http) {
        return RecordServerEventCommand.builder()
            .level("INFO")
            .event(event)
            .contributorId(contributorId)
            .sessionId(sessionId)
            .method(method)
            .path(path)
            .httpStatus(200)
            .useCase("ClientTelemetry")
            .correlationId(attrUuid(http, CorrelationIdFilter.ATTR_CORRELATION_ID))
            .requestId(attrUuid(http, CorrelationIdFilter.ATTR_REQUEST_ID));
    }

    private static UUID attrUuid(HttpServletRequest http, String attr) {
        Object v = http.getAttribute(attr);
        if (v == null) return null;
        try {
            return UUID.fromString(v.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
