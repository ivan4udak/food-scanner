package com.foodscanner.application.result;

import java.util.UUID;

/** Результат успешной аутентификации: профиль + пара токенов. */
public record AuthSession(UUID contributorId, String username,
                          String accessToken, String refreshToken) {}
