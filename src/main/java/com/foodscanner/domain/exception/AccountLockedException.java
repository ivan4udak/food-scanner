package com.foodscanner.domain.exception;

/** Аккаунт временно заблокирован после серии неудачных входов. */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("Аккаунт временно заблокирован");
    }
}
