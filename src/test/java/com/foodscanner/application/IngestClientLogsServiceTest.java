package com.foodscanner.application;

import com.foodscanner.application.command.IngestClientLogsCommand;
import com.foodscanner.application.command.IngestClientLogsCommand.LogLine;
import com.foodscanner.application.result.IngestClientLogsResult;
import com.foodscanner.application.service.HealthNoisePolicy;
import com.foodscanner.application.service.IngestClientLogsService;
import com.foodscanner.application.service.TelemetrySanitizer;
import com.foodscanner.domain.model.telemetry.ClientLogEntry;
import com.foodscanner.domain.repository.ClientLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IngestClientLogsServiceTest {

    private final ClientLogRepository repository = mock(ClientLogRepository.class);
    private final IngestClientLogsService service =
        new IngestClientLogsService(repository, new TelemetrySanitizer(), new HealthNoisePolicy());

    @Test
    void filtersHealthNoiseAndMasksSecretsAndCountsAccepted() {
        UUID contributor = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        LogLine scan = LogLine.builder()
            .level("INFO").category("SCAN").event("SCAN_RESULT").message("Scan result NEW")
            .timestamp(Instant.now()).metadata(Map.of("password", "p", "barcode", "460")).build();
        LogLine pingOk = LogLine.builder()
            .level("INFO").category("NETWORK").event("PING_OK")
            .apiPath("/api/v1/ping").httpStatus(200).timestamp(Instant.now()).build();
        LogLine pingFail = LogLine.builder()
            .level("ERROR").category("NETWORK").event("PING_FAILED")
            .apiPath("/api/v1/ping").httpStatus(503).timestamp(Instant.now()).build();

        IngestClientLogsResult result = service.execute(new IngestClientLogsCommand(
            contributor, session, "1.7.0", "1.7.0", Instant.now(), List.of(scan, pingOk, pingFail)));

        assertThat(result.accepted()).isEqualTo(2); // pingOk отфильтрован

        ArgumentCaptor<List<ClientLogEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ClientLogEntry> saved = captor.getValue();
        assertThat(saved).hasSize(2);

        ClientLogEntry scanEntry = saved.stream().filter(e -> "SCAN".equals(e.category())).findFirst().orElseThrow();
        assertThat(scanEntry.metadata().get("password")).isEqualTo("********");
        assertThat(scanEntry.metadata().get("barcode")).isEqualTo("460");
        assertThat(scanEntry.contributorId()).isEqualTo(contributor);
        assertThat(scanEntry.id()).isNotNull();
    }

    @Test
    void emptyBatchSavesNothing() {
        IngestClientLogsResult result = service.execute(new IngestClientLogsCommand(
            UUID.randomUUID(), UUID.randomUUID(), "1.7.0", "1.7.0", Instant.now(), List.of()));
        assertThat(result.accepted()).isZero();
        verify(repository, never()).saveAll(any());
    }
}
