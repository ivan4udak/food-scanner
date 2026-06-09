package com.foodscanner.application.service;

import com.foodscanner.application.port.AdminReadPort;
import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;
import com.foodscanner.application.usecase.AdminReadUseCase;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * Read-facade админки: окна времени (today/week/online), сборка карточек,
 * слияние сквозной трассировки client_logs + server_events.
 */
@Service
public class AdminQueryService implements AdminReadUseCase {

    /** Онлайн — активность за последние 5 минут. */
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);
    private static final int SESSIONS_LIMIT = 20;
    private static final int RECENT_SCANS_LIMIT = 50;

    private final AdminReadPort port;

    public AdminQueryService(AdminReadPort port) {
        this.port = port;
    }

    @Override
    public AdminDashboard dashboard() {
        Instant now = Instant.now();
        return port.dashboard(todayStart(), now.minus(Duration.ofDays(7)), now.minus(ONLINE_WINDOW));
    }

    @Override
    public List<AdminUserRow> users(String sort, int limit, int offset) {
        return port.users(onlineSince(), sort, clampLimit(limit), Math.max(0, offset));
    }

    @Override
    public Optional<AdminUserDetail> userDetail(UUID id) {
        return port.user(id, onlineSince()).map(this::assembleDetail);
    }

    @Override
    public Optional<AdminUserDetail> userDetailByUsername(String username) {
        return port.userByUsername(username, onlineSince()).map(this::assembleDetail);
    }

    private AdminUserDetail assembleDetail(AdminUserRow user) {
        return new AdminUserDetail(
            user,
            port.sessions(user.id(), SESSIONS_LIMIT),
            port.scans(user.id(), RECENT_SCANS_LIMIT));
    }

    @Override
    public List<AdminClientLog> userLogs(UUID id, int limit, int offset) {
        return port.clientLogs(AdminLogFilter.builder()
            .contributorId(id).limit(clampLimit(limit)).offset(Math.max(0, offset)).build());
    }

    @Override
    public List<AdminClientLog> logs(AdminLogFilter filter) {
        return port.clientLogs(filter);
    }

    @Override
    public List<AdminClientLog> clientErrors(int limit) {
        return port.clientErrors(todayStart(), clampLimit(limit));
    }

    @Override
    public List<AdminServerEventRow> serverErrors(int limit) {
        return port.serverErrors(todayStart(), clampLimit(limit));
    }

    @Override
    public List<AdminCatalogRow> catalog(int limit, int offset) {
        return port.catalog(clampLimit(limit), Math.max(0, offset));
    }

    @Override
    public Optional<AdminCatalogDetail> catalogDetail(String barcode) {
        return port.catalogByBarcode(barcode).map(row -> new AdminCatalogDetail(
            row.catalogEntryId(), row.barcode(), row.contributorId(), row.author(), row.createdAt(),
            port.catalogPhotos(row.catalogEntryId()),
            port.clientLogsByBarcode(barcode, 200)));
    }

    @Override
    public List<TraceItem> trace(UUID correlationId) {
        List<TraceItem> items = new ArrayList<>();
        for (AdminClientLog c : port.clientLogsByCorrelation(correlationId)) {
            items.add(new TraceItem("CLIENT", c.timestamp(), c.level(), c.category(), c.event(),
                c.message(), c.apiMethod(), c.apiPath(), c.httpStatus(), c.durationMs()));
        }
        for (AdminServerEventRow s : port.serverEventsByCorrelation(correlationId)) {
            items.add(new TraceItem("SERVER", s.occurredAt(), s.level(), s.useCase(), s.event(),
                s.errorMessage(), s.method(), s.path(), s.httpStatus(), s.durationMs()));
        }
        items.sort(Comparator.comparing(TraceItem::at, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    // ── helpers ──────────────────────────────────────────────
    private static Instant todayStart() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
    private static Instant onlineSince() {
        return Instant.now().minus(ONLINE_WINDOW);
    }
    private static int clampLimit(int limit) {
        return limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
