package com.foodscanner.api.controller;

import com.foodscanner.api.dto.*;
import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.result.AuthSession;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.usecase.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Слой: api
 *
 *   POST /api/v1/auth/login    — вход (200 OK / 200 RECOVERY / 401 / 404 / 423)
 *   POST /api/v1/auth/register — создание аккаунта (201 / 409)
 *   POST /api/v1/auth/recover  — установка нового пароля в окне восстановления (200 / 410)
 *   POST /api/v1/auth/refresh  — обновление токенов (200 / 401)
 *
 * Успех возвращает пару токенов: accessToken (JWT, 24ч) + refreshToken (30д).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase auth;

    public AuthController(AuthUseCase auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResult r = auth.login(new LoginCommand(req.username(), req.password()));
        return switch (r.getStatus()) {
            case OK       -> ResponseEntity.ok(AuthResponse.ok(r.getSession()));
            case RECOVERY -> ResponseEntity.ok(AuthResponse.recovery(r.getUsername()));
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.status("NOT_FOUND", "Пользователь не найден"));
            case INVALID_CREDENTIALS -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.status("INVALID", "Неверный логин или пароль"));
            case LOCKED -> ResponseEntity.status(HttpStatus.LOCKED)
                    .body(AuthResponse.status("LOCKED", "Аккаунт временно заблокирован"));
        };
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterAccountRequest req) {
        AuthSession s = auth.register(new RegisterAccountCommand(req.username(), req.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.ok(s));
    }

    @PostMapping("/recover")
    public ResponseEntity<AuthResponse> recover(@Valid @RequestBody RecoverPasswordRequest req) {
        AuthSession s = auth.recoverPassword(new RecoverPasswordCommand(req.username(), req.password()));
        return ResponseEntity.ok(AuthResponse.ok(s));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        AuthSession s = auth.refresh(req.refreshToken());
        return ResponseEntity.ok(AuthResponse.ok(s));
    }
}
