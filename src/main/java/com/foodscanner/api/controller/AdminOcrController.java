package com.foodscanner.api.controller;

import com.foodscanner.application.usecase.ReprocessOcrUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Слой: api. OCR-действия админки (write). Гард /api/v1/admin/**.
 *   POST /admin/ocr/{jobId}/reprocess — переотправить фото в OCR (для статусов 3/5).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminOcrController {

    private final ReprocessOcrUseCase reprocessOcr;

    public AdminOcrController(ReprocessOcrUseCase reprocessOcr) {
        this.reprocessOcr = reprocessOcr;
    }

    @PostMapping("/ocr/{jobId}/reprocess")
    public ResponseEntity<Map<String, String>> reprocess(@PathVariable UUID jobId) {
        try {
            return reprocessOcr.execute(jobId)
                .map(id -> ResponseEntity.ok(Map.of("jobId", id.toString())))
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
