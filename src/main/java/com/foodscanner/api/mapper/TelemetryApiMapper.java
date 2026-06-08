package com.foodscanner.api.mapper;

import com.foodscanner.api.dto.telemetry.ClientActivityRequest;
import com.foodscanner.api.dto.telemetry.ClientLogBatchRequest;
import com.foodscanner.api.dto.telemetry.ClientLogDto;
import com.foodscanner.api.dto.telemetry.ClientSessionRequest;
import com.foodscanner.application.command.IngestClientLogsCommand;
import com.foodscanner.application.command.IngestClientLogsCommand.LogLine;
import com.foodscanner.application.command.RecordClientActivityCommand;
import com.foodscanner.application.command.RecordClientSessionCommand;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Слой: api.
 * Преобразует входящие DTO телеметрии в application-команды. Толерантный парсинг
 * времени/UUID — плохие значения становятся null, запрос не падает.
 */
@Component
public class TelemetryApiMapper {

    public IngestClientLogsCommand toCommand(ClientLogBatchRequest req, UUID contributorId, Instant receivedAt) {
        List<LogLine> lines = req.logs() == null ? List.of()
            : req.logs().stream().map(TelemetryApiMapper::toLogLine).toList();
        return new IngestClientLogsCommand(
            contributorId, req.sessionId(), req.clientVersion(), req.pwaVersion(), receivedAt, lines);
    }

    private static LogLine toLogLine(ClientLogDto d) {
        return LogLine.builder()
            .clientLogId(d.id())
            .timestamp(parseInstant(d.timestamp()))
            .level(d.level())
            .category(d.category())
            .event(d.event())
            .message(d.message())
            .screen(d.screen())
            .metadata(d.metadata())
            .durationMs(d.durationMs())
            .stackTrace(d.stackTrace())
            .correlationId(parseUuid(d.correlationId()))
            .requestId(parseUuid(d.requestId()))
            .barcode(d.barcode())
            .draftId(parseUuid(d.draftId()))
            .catalogEntryId(parseUuid(d.catalogEntryId()))
            .photoId(parseUuid(d.photoId()))
            .apiMethod(d.apiMethod())
            .apiPath(d.apiPath())
            .httpStatus(d.httpStatus())
            .build();
    }

    public RecordClientSessionCommand toCommand(ClientSessionRequest r, UUID contributorId, Instant receivedAt) {
        return new RecordClientSessionCommand(
            contributorId, r.sessionId(), receivedAt, r.clientVersion(), r.pwaVersion(), r.browser(),
            r.os(), r.deviceType(), r.language(), r.timezone(), r.screenWidth(), r.screenHeight(),
            r.hardwareConcurrency(), r.deviceMemory(), r.networkStatus(), r.standalone());
    }

    public RecordClientActivityCommand toCommand(ClientActivityRequest r, UUID contributorId) {
        return new RecordClientActivityCommand(
            contributorId, r.sessionId(), r.screen(), r.online(), parseInstant(r.timestamp()));
    }

    // ── helpers ──────────────────────────────────────────────
    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
