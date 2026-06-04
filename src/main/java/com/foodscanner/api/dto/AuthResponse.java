package com.foodscanner.api.dto;

import com.foodscanner.application.result.AuthSession;
import java.util.UUID;

/** Ответ сценариев входа/создания/восстановления/обновления токена. */
public record AuthResponse(String status, UUID contributorId, String username,
                           String accessToken, String refreshToken, String message) {

    public static AuthResponse ok(AuthSession s) {
        return new AuthResponse("OK", s.contributorId(), s.username(),
            s.accessToken(), s.refreshToken(), null);
    }
    public static AuthResponse recovery(String username) {
        return new AuthResponse("RECOVERY", null, username, null, null, "Требуется новый пароль");
    }
    public static AuthResponse status(String status, String message) {
        return new AuthResponse(status, null, null, null, null, message);
    }
}
