package com.foodscanner.application;

import com.foodscanner.application.command.RecordClientSessionCommand;
import com.foodscanner.application.service.RecordClientSessionService;
import com.foodscanner.domain.model.telemetry.ClientSession;
import com.foodscanner.domain.repository.ClientSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecordClientSessionServiceTest {

    private final ClientSessionRepository repository = mock(ClientSessionRepository.class);
    private final RecordClientSessionService service = new RecordClientSessionService(repository);

    private RecordClientSessionCommand cmd(UUID session, Instant receivedAt) {
        return new RecordClientSessionCommand(UUID.randomUUID(), session, receivedAt,
            "1.7.0", "1.7.0", "Safari", "iOS", "mobile", "ru-RU", "Europe/Helsinki",
            390, 844, 6, 4.0, "online", true);
    }

    @Test
    void newSessionUsesReceivedAtAsStart() {
        UUID session = UUID.randomUUID();
        Instant now = Instant.now();
        when(repository.findBySessionId(session)).thenReturn(Optional.empty());

        service.execute(cmd(session, now));

        ArgumentCaptor<ClientSession> captor = ArgumentCaptor.forClass(ClientSession.class);
        verify(repository).upsert(captor.capture());
        ClientSession s = captor.getValue();
        assertThat(s.startedAt()).isEqualTo(now);
        assertThat(s.lastSeenAt()).isEqualTo(now);
        assertThat(s.id()).isNotNull();
    }

    @Test
    void existingSessionKeepsStartedAtAndId() {
        UUID session = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        Instant t0 = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant now = Instant.now();
        ClientSession existing = ClientSession.builder()
            .id(existingId).sessionId(session).startedAt(t0).lastSeenAt(t0).build();
        when(repository.findBySessionId(session)).thenReturn(Optional.of(existing));

        service.execute(cmd(session, now));

        ArgumentCaptor<ClientSession> captor = ArgumentCaptor.forClass(ClientSession.class);
        verify(repository).upsert(captor.capture());
        ClientSession s = captor.getValue();
        assertThat(s.id()).isEqualTo(existingId);
        assertThat(s.startedAt()).isEqualTo(t0);
        assertThat(s.lastSeenAt()).isEqualTo(now);
    }
}
