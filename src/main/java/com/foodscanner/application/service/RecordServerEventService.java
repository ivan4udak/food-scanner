package com.foodscanner.application.service;

import com.foodscanner.application.command.RecordServerEventCommand;
import com.foodscanner.application.usecase.RecordServerEventUseCase;
import com.foodscanner.domain.model.telemetry.ServerEvent;
import com.foodscanner.domain.repository.ServerEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application.
 * Сохранение значимого серверного события: отсев heartbeat-шума (успешные
 * ping/health) + маскировка metadata/errorMessage.
 */
@Service
public class RecordServerEventService implements RecordServerEventUseCase {

    private final ServerEventRepository repository;
    private final TelemetrySanitizer sanitizer;
    private final HealthNoisePolicy healthNoise;

    public RecordServerEventService(ServerEventRepository repository,
                                    TelemetrySanitizer sanitizer,
                                    HealthNoisePolicy healthNoise) {
        this.repository = repository;
        this.sanitizer = sanitizer;
        this.healthNoise = healthNoise;
    }

    @Override
    public void execute(RecordServerEventCommand c) {
        if (healthNoise.isNoise(c.path(), c.httpStatus(), c.level())) {
            return;
        }
        ServerEvent event = ServerEvent.builder()
            .id(UUID.randomUUID())
            .occurredAt(Instant.now())
            .level(c.level())
            .event(c.event())
            .correlationId(c.correlationId())
            .requestId(c.requestId())
            .contributorId(c.contributorId())
            .username(c.username())
            .sessionId(c.sessionId())
            .method(c.method())
            .path(c.path())
            .httpStatus(c.httpStatus())
            .durationMs(c.durationMs())
            .useCase(c.useCase())
            .barcode(c.barcode())
            .draftId(c.draftId())
            .catalogEntryId(c.catalogEntryId())
            .photoId(c.photoId())
            .errorCode(c.errorCode())
            .errorMessage(sanitizer.maskString(c.errorMessage()))
            .exceptionClass(c.exceptionClass())
            .metadata(sanitizer.maskMap(c.metadata()))
            .build();

        repository.save(event);
    }
}
