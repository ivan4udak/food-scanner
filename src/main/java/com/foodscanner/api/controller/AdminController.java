package com.foodscanner.api.controller;

import com.foodscanner.api.dto.AdminResetPasswordRequest;
import com.foodscanner.api.dto.AuthResponse;
import com.foodscanner.application.command.AdminResetPasswordCommand;
import com.foodscanner.application.usecase.AdminUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Слой: api
 * POST /api/v1/admin/reset-password — админский сброс пароля.
 * 200 OK / 403 Forbidden (неверная роль/админ-пароль) / 404 (логин не найден).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminUseCase admin;

    public AdminController(AdminUseCase admin) {
        this.admin = admin;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody AdminResetPasswordRequest req) {
        admin.resetPassword(new AdminResetPasswordCommand(req.role(), req.password(), req.username()));
        return ResponseEntity.ok(AuthResponse.status("RESET", "Пароль сброшен, окно восстановления 5 минут"));
    }
}
