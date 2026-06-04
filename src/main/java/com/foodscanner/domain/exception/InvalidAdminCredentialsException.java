package com.foodscanner.domain.exception;

/** Неверная роль или админ-пароль при админском сбросе пароля. */
public class InvalidAdminCredentialsException extends RuntimeException {
    public InvalidAdminCredentialsException() {
        super("Доступ запрещён");
    }
}
