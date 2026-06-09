package com.foodscanner.domain.exception;

/**
 * Слой: domain.
 * Доступ запрещён: аутентифицированный пользователь не имеет нужной роли.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
