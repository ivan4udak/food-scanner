package com.foodscanner.application.port;

import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (порт чтения админки). Реализация — нативный SQL в infrastructure.
 */
public interface AdminReadPort {

    AdminDashboard dashboard(Instant todayStart, Instant weekStart, Instant onlineSince);

    List<AdminUserRow> users(Instant onlineSince, String sort, int limit, int offset);

    Optional<AdminUserRow> user(UUID id, Instant onlineSince);

    Optional<AdminUserRow> userByUsername(String username, Instant onlineSince);

    List<AdminSessionRow> sessions(UUID contributorId, int limit);

    List<AdminScanRow> scans(UUID contributorId, int limit);

    List<AdminClientLog> clientLogs(AdminLogFilter filter);

    List<AdminClientLog> clientLogsByCorrelation(UUID correlationId);

    List<AdminClientLog> clientLogsByBarcode(String barcode, int limit);

    List<AdminClientLog> clientErrors(Instant since, int limit);

    List<AdminClientLog> clientErrorsByUser(UUID contributorId, int limit);

    List<AdminServerEventRow> serverEventsByCorrelation(UUID correlationId);

    List<AdminServerEventRow> serverErrors(Instant since, int limit);

    List<AdminCatalogRow> catalog(int limit, int offset);

    Optional<AdminCatalogRow> catalogByBarcode(String barcode);

    List<AdminCatalogDetail.Photo> catalogPhotos(UUID catalogEntryId);

    /** OCR-задачи (фильтры опциональны): свежие сверху. */
    List<AdminOcrRow> ocrJobs(Integer status, String barcode, int limit, int offset);

    /** Сводка OCR-задач по статусам (zero-fill). */
    AdminOcrSummary ocrSummary();
}
