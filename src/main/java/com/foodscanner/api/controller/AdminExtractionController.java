package com.foodscanner.api.controller;

import com.foodscanner.application.usecase.RequeueExtractionUseCase;
import com.foodscanner.application.usecase.SkipExtractionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Слой: api. Действия админки над задачами извлечения (write). Гард /api/v1/admin/**.
 *   POST /admin/extraction/{jobId}/requeue — новая QUEUED для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5).
 *   POST /admin/extraction/{jobId}/skip    — → SKIPPED для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4).
 * Запрещённый статус → 409 Conflict; нет задачи → 404.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminExtractionController {

    private final RequeueExtractionUseCase requeue;
    private final SkipExtractionUseCase skip;

    public AdminExtractionController(RequeueExtractionUseCase requeue, SkipExtractionUseCase skip) {
        this.requeue = requeue;
        this.skip = skip;
    }

    @PostMapping("/extraction/{jobId}/requeue")
    public ResponseEntity<Map<String, String>> requeue(@PathVariable UUID jobId) {
        try {
            return requeue.execute(jobId)
                .map(id -> ResponseEntity.ok(Map.of("jobId", id.toString())))
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/extraction/{jobId}/skip")
    public ResponseEntity<Map<String, String>> skip(@PathVariable UUID jobId) {
        try {
            return skip.execute(jobId)
                .map(id -> ResponseEntity.ok(Map.of("jobId", id.toString())))
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
