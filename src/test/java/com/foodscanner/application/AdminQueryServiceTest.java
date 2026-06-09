package com.foodscanner.application;

import com.foodscanner.application.port.AdminReadPort;
import com.foodscanner.application.result.admin.*;
import com.foodscanner.application.service.AdminQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminQueryServiceTest {

    private final AdminReadPort port = mock(AdminReadPort.class);
    private final AdminQueryService service = new AdminQueryService(port);

    @Test
    void traceMergesClientAndServerSortedByTime() {
        UUID corr = UUID.randomUUID();
        Instant t0 = Instant.parse("2026-06-08T10:00:00Z");
        Instant t1 = t0.plusMillis(250);
        when(port.clientLogsByCorrelation(corr)).thenReturn(List.of(
            new AdminClientLog(UUID.randomUUID(), null, "ivan", null, corr, t1, "INFO", "NETWORK",
                "API_RESPONSE", "ScannerPage", "← 200", null, 180L, null, "460", "POST", "/api/v1/scan", 200)));
        when(port.serverEventsByCorrelation(corr)).thenReturn(List.of(
            new AdminServerEventRow(UUID.randomUUID(), t0, "INFO", "SCAN_COMPLETED", corr, null, "ivan",
                "POST", "/api/v1/scan", 200, 60L, "ScanBarcode", "460", null, null, null)));

        List<TraceItem> trace = service.trace(corr);

        assertThat(trace).hasSize(2);
        assertThat(trace.get(0).source()).isEqualTo("SERVER"); // t0 раньше
        assertThat(trace.get(0).event()).isEqualTo("SCAN_COMPLETED");
        assertThat(trace.get(1).source()).isEqualTo("CLIENT");
    }

    @Test
    void userDetailAssemblesSessionsAndScans() {
        UUID id = UUID.randomUUID();
        AdminUserRow row = new AdminUserRow(id, "ivan", "USER", true, Instant.now(),
            "1.7.0", "Safari", "iOS", "mobile", 3, 1, 5, 0);
        when(port.user(eq(id), any())).thenReturn(Optional.of(row));
        when(port.sessions(eq(id), anyInt())).thenReturn(List.of());
        when(port.scans(eq(id), anyInt())).thenReturn(List.of());

        Optional<AdminUserDetail> detail = service.userDetail(id);

        assertThat(detail).isPresent();
        assertThat(detail.get().user().username()).isEqualTo("ivan");
    }

    @Test
    void userDetailEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(port.user(eq(id), any())).thenReturn(Optional.empty());
        assertThat(service.userDetail(id)).isEmpty();
    }

    @Test
    void userDetailByUsernameAssembles() {
        UUID id = UUID.randomUUID();
        AdminUserRow row = new AdminUserRow(id, "ivan", "USER", true, Instant.now(),
            "1.8.0", "Safari", "iOS", "mobile", 3, 1, 5, 0);
        when(port.userByUsername(eq("ivan"), any())).thenReturn(Optional.of(row));
        when(port.sessions(eq(id), anyInt())).thenReturn(List.of());
        when(port.scans(eq(id), anyInt())).thenReturn(List.of());

        assertThat(service.userDetailByUsername("ivan")).isPresent()
            .get().extracting(d -> d.user().username()).isEqualTo("ivan");
    }

    @Test
    void usersClampsLimit() {
        when(port.users(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        service.users("completedEntries", 5000, 0);
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(port).users(any(), eq("completedEntries"), limit.capture(), eq(0));
        assertThat(limit.getValue()).isEqualTo(500);
    }

    @Test
    void dashboardUsesOnlineWindow() {
        when(port.dashboard(any(), any(), any())).thenReturn(
            new AdminDashboard(0,0,0,0,0,0,0,0,0,0,0));
        service.dashboard();
        ArgumentCaptor<Instant> online = ArgumentCaptor.forClass(Instant.class);
        verify(port).dashboard(any(), any(), online.capture());
        // онлайн-окно ~5 минут назад
        assertThat(online.getValue()).isBefore(Instant.now().minus(4, ChronoUnit.MINUTES));
    }
}
