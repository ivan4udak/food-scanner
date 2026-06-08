package com.foodscanner.api.controller;

import com.foodscanner.application.result.me.MeScanDetail;
import com.foodscanner.application.result.me.MeScanRow;
import com.foodscanner.application.usecase.MyScansUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Слой: api.
 *
 * «Мои сканы» (Bearer): пользователь видит только свои ШК и фото.
 *   GET /api/v1/me/scans            — список своих сканов
 *   GET /api/v1/me/scans/{barcode}  — детали скана с фото (thumb/full URL)
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeScansController {

    private static final String AUTH_CONTRIBUTOR = "authContributorId";

    private final MyScansUseCase myScans;

    public MeScansController(MyScansUseCase myScans) {
        this.myScans = myScans;
    }

    @GetMapping("/scans")
    public List<MeScanRow> scans(@RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {
        return myScans.list(contributorId);
    }

    @GetMapping("/scans/{barcode}")
    public ResponseEntity<MeScanDetail> scan(
            @PathVariable String barcode,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {
        return myScans.detail(contributorId, barcode)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
