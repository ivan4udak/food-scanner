package com.foodscanner.api.dto;

import java.util.UUID;

/** Ответ сценариев входа/создания/восстановления. */
public record AuthResponse(String status, UUID contributorId, String username, String message) {
    public static AuthResponse ok(UUID id, String username) {
        return new AuthResponse("OK", id, username, null);
    }
    public static AuthResponse recovery(String username) {
        return new AuthResponse("RECOVERY", null, username, "Требуется новый пароль");
    }
    public static AuthResponse status(String status, String message) {
        return new AuthResponse(status, null, null, message);
    }
}
