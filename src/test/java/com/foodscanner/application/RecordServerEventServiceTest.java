package com.foodscanner.application;

import com.foodscanner.application.command.RecordServerEventCommand;
import com.foodscanner.application.service.HealthNoisePolicy;
import com.foodscanner.application.service.RecordServerEventService;
import com.foodscanner.application.service.TelemetrySanitizer;
import com.foodscanner.domain.model.telemetry.ServerEvent;
import com.foodscanner.domain.repository.ServerEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecordServerEventServiceTest {

    private final ServerEventRepository repository = mock(ServerEventRepository.class);
    private final RecordServerEventService service =
        new RecordServerEventService(repository, new TelemetrySanitizer(), new HealthNoisePolicy());

    @Test
    void dropsSuccessfulHealthEvent() {
        service.execute(RecordServerEventCommand.builder()
            .event("HEALTH_COMPLETED").path("/api/v1/health").httpStatus(200).build());
        verify(repository, never()).save(any());
    }

    @Test
    void savesBusinessEventAndMasksMetadata() {
        UUID corr = UUID.randomUUID();
        service.execute(RecordServerEventCommand.builder()
            .event("SCAN_COMPLETED").correlationId(corr).barcode("460").httpStatus(200)
            .metadata(Map.of("token", "secret")).build());

        ArgumentCaptor<ServerEvent> captor = ArgumentCaptor.forClass(ServerEvent.class);
        verify(repository).save(captor.capture());
        ServerEvent e = captor.getValue();
        assertThat(e.event()).isEqualTo("SCAN_COMPLETED");
        assertThat(e.correlationId()).isEqualTo(corr);
        assertThat(e.id()).isNotNull();
        assertThat(e.occurredAt()).isNotNull();
        assertThat(e.metadata().get("token")).isEqualTo("********");
    }
}
