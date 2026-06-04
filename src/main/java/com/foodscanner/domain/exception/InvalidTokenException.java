package com.foodscanner.domain.exception;

/** Невалидный или просроченный токен (access/refresh). → HTTP 401. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}
