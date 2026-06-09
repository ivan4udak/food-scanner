package com.foodscanner.application.service;

import com.foodscanner.application.command.IngestClientLogsCommand;
import com.foodscanner.application.command.IngestClientLogsCommand.LogLine;
import com.foodscanner.application.result.IngestClientLogsResult;
import com.foodscanner.application.usecase.IngestClientLogsUseCase;
import com.foodscanner.domain.model.telemetry.ClientLogEntry;
import com.foodscanner.domain.repository.ClientLogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Слой: application.
 *
 * Приём партии клиентских логов:
 *  1) отсев heartbeat-шума (успешные ping/health не сохраняются);
 *  2) повторная маскировка секретов (message/stackTrace/metadata);
 *  3) маппинг в доменную модель и сохранение пачкой.
 */
@Service
public class IngestClientLogsService implements IngestClientLogsUseCase {

    private final ClientLogRepository repository;
    private final TelemetrySanitizer sanitizer;
    private final HealthNoisePolicy healthNoise;

    public IngestClientLogsService(ClientLogRepository repository,
                                   TelemetrySanitizer sanitizer,
                                   HealthNoisePolicy healthNoise) {
        this.repository = repository;
        this.sanitizer = sanitizer;
        this.healthNoise = healthNoise;
    }

    @Override
    public IngestClientLogsResult execute(IngestClientLogsCommand command) {
        List<ClientLogEntry> toSave = new ArrayList<>();
        if (command.logs() != null) {
            for (LogLine line : command.logs()) {
                if (line == null) continue;
                if (healthNoise.isNoise(line.apiPath(), line.httpStatus(), line.level())) {
                    continue;
                }
                toSave.add(toEntry(command, line));
            }
        }
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        return new IngestClientLogsResult(toSave.size());
    }

    private ClientLogEntry toEntry(IngestClientLogsCommand cmd, LogLine line) {
        return ClientLogEntry.builder()
            .id(UUID.randomUUID())
            .contributorId(cmd.contributorId())
            .sessionId(cmd.sessionId())
            .clientLogId(line.clientLogId())
            .correlationId(line.correlationId())
            .requestId(line.requestId())
            .timestamp(line.timestamp())
            .receivedAt(cmd.receivedAt())
            .level(line.level())
            .category(line.category())
            .event(line.event())
            .message(sanitizer.maskString(line.message()))
            .screen(line.screen())
            .metadata(sanitizer.maskMap(line.metadata()))
            .durationMs(line.durationMs())
            .stackTrace(sanitizer.maskString(line.stackTrace()))
            .barcode(line.barcode())
            .draftId(line.draftId())
            .catalogEntryId(line.catalogEntryId())
            .photoId(line.photoId())
            .apiMethod(line.apiMethod())
            .apiPath(line.apiPath())
            .httpStatus(line.httpStatus())
            .clientVersion(cmd.clientVersion())
            .pwaVersion(cmd.pwaVersion())
            .build();
    }
}
