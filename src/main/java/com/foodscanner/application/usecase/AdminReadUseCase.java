package com.foodscanner.application.usecase;

import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (use case).
 * Read-facade админ-панели: сводка, пользователи, логи, каталог, сквозная трассировка.
 * Только чтение — операционная наблюдаемость, без изменения данных.
 */
public interface AdminReadUseCase {

    AdminDashboard dashboard();

    List<AdminUserRow> users(String sort, int limit, int offset);

    Optional<AdminUserDetail> userDetail(UUID id);

    Optional<AdminUserDetail> userDetailByUsername(String username);

    List<AdminClientLog> userLogs(UUID id, int limit, int offset);

    List<AdminClientLog> userErrors(UUID id, int limit);

    List<AdminClientLog> logs(AdminLogFilter filter);

    List<AdminClientLog> clientErrors(int limit);

    List<AdminServerEventRow> serverErrors(int limit);

    List<AdminCatalogRow> catalog(int limit, int offset);

    Optional<AdminCatalogDetail> catalogDetail(String barcode);

    List<TraceItem> trace(UUID correlationId);
}
