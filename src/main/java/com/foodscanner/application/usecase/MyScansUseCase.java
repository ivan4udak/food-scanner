package com.foodscanner.application.usecase;

import com.foodscanner.application.result.me.MeScanDetail;
import com.foodscanner.application.result.me.MeScanRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application (use case).
 * «Мои сканы»: список ШК пользователя и детали с фото. Только свои данные.
 */
public interface MyScansUseCase {

    List<MeScanRow> list(UUID contributorId);

    Optional<MeScanDetail> detail(UUID contributorId, String barcode);
}
