package com.foodscanner.domain.exception;

/** Установка нового пароля вне активного окна восстановления. */
public class RecoveryNotAllowedException extends RuntimeException {
    public RecoveryNotAllowedException() {
        super("Окно восстановления пароля недоступно");
    }
}
