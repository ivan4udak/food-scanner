package com.foodscanner.api.controller;

import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;
import com.foodscanner.application.usecase.AdminReadUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Слой: api.
 *
 * Read-эндпоинты админ-панели (доступ — ADMIN/SUPER_ADMIN, гард на /api/v1/admin/**):
 *   GET /admin/dashboard
 *   GET /admin/users  · GET /admin/users/{id} · GET /admin/users/{id}/logs
 *   GET /admin/logs   · GET /admin/errors
 *   GET /admin/catalog · GET /admin/catalog/{barcode}
 *   GET /admin/trace/{correlationId}   (client_logs + server_events в одной линии)
 *
 * Возвращает application-result записи как JSON (это не доменные/JPA-сущности).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminReadController {

    private final AdminReadUseCase admin;

    public AdminReadController(AdminReadUseCase admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    public AdminDashboard dashboard() {
        return admin.dashboard();
    }

    @GetMapping("/users")
    public List<AdminUserRow> users(
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return admin.users(sort, limit, offset);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetail> user(@PathVariable UUID id) {
        return admin.userDetail(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Поиск пользователя по нику (для перехода из публичного рейтинга /stats). */
    @GetMapping("/users/by-username/{username}")
    public ResponseEntity<AdminUserDetail> userByName(@PathVariable String username) {
        return admin.userDetailByUsername(username).map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/logs")
    public List<AdminClientLog> userLogs(
            @PathVariable UUID id,
            @RequestParam(name = "limit", defaultValue = "200") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return admin.userLogs(id, limit, offset);
    }

    /** Только ошибки (WARN/ERROR) клиента конкретного пользователя. */
    @GetMapping("/users/{id}/errors")
    public List<AdminClientLog> userErrors(
            @PathVariable UUID id,
            @RequestParam(name = "limit", defaultValue = "200") int limit) {
        return admin.userErrors(id, limit);
    }

    @GetMapping("/logs")
    public List<AdminClientLog> logs(
            @RequestParam(name = "contributorId", required = false) UUID contributorId,
            @RequestParam(name = "sessionId", required = false) UUID sessionId,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "event", required = false) String event,
            @RequestParam(name = "barcode", required = false) String barcode,
            @RequestParam(name = "screen", required = false) String screen,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return admin.logs(AdminLogFilter.builder()
            .contributorId(contributorId).sessionId(sessionId).level(blankToNull(level))
            .category(blankToNull(category)).event(blankToNull(event)).barcode(blankToNull(barcode))
            .screen(blankToNull(screen)).dateFrom(parseInstant(dateFrom)).dateTo(parseInstant(dateTo))
            .limit(limit).offset(offset).build());
    }

    @GetMapping("/errors")
    public AdminErrors errors(@RequestParam(name = "limit", defaultValue = "200") int limit) {
        return new AdminErrors(admin.clientErrors(limit), admin.serverErrors(limit));
    }

    @GetMapping("/catalog")
    public List<AdminCatalogRow> catalog(
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return admin.catalog(limit, offset);
    }

    @GetMapping("/catalog/{barcode}")
    public ResponseEntity<AdminCatalogDetail> catalogDetail(@PathVariable String barcode) {
        return admin.catalogDetail(barcode).map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/trace/{correlationId}")
    public List<TraceItem> trace(@PathVariable UUID correlationId) {
        return admin.trace(correlationId);
    }

    /** Список OCR-задач (фильтры status/barcode опциональны), свежие сверху. */
    @GetMapping("/ocr")
    public List<AdminOcrRow> ocr(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "barcode", required = false) String barcode,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return admin.ocr(status, blankToNull(barcode), limit, offset);
    }

    /** Сводка OCR-задач по статусам (для шапки страницы). */
    @GetMapping("/ocr/summary")
    public AdminOcrSummary ocrSummary() {
        return admin.ocrSummary();
    }

    // ── helpers ──────────────────────────────────────────────
    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
