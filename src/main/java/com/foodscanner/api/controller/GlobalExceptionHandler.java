package com.foodscanner.api.controller;

import com.foodscanner.api.dto.ErrorResponse;
import com.foodscanner.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Слой: api
 *
 * Централизованная обработка исключений.
 *
 * Доменные исключения → HTTP статусы:
 *   ContributorAlreadyExistsException    → 409 Conflict
 *   CatalogEntryAlreadyExistsException   → 409 Conflict
 *   CatalogDraftNotFoundException        → 404 Not Found
 *   CatalogNotCompletableException       → 422 Unprocessable Entity
 *   IllegalStateException                → 422 Unprocessable Entity
 *   IllegalArgumentException             → 400 Bad Request
 *   NoResourceFoundException             → 404 Not Found   ← Spring 6.1+
 *   HttpRequestMethodNotSupportedException → 405 Method Not Allowed
 *   MethodArgumentNotValidException      → 400 Bad Request
 *   Exception                            → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContributorAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleContributorAlreadyExists(
            ContributorAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(CatalogEntryAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCatalogEntryAlreadyExists(
            CatalogEntryAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(CatalogDraftNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDraftNotFound(
            CatalogDraftNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(CatalogNotCompletableException.class)
    public ResponseEntity<ErrorResponse> handleNotCompletable(
            CatalogNotCompletableException ex) {
        List<String> missing = ex.getMissingTypes().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage(), missing));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
            .body(new ErrorResponse(423, "Locked", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(com.foodscanner.domain.exception.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            com.foodscanner.domain.exception.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(InvalidAdminCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleAdmin(InvalidAdminCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(RecoveryNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleRecovery(RecoveryNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(new ErrorResponse(410, "Gone", ex.getMessage()));
    }

    @ExceptionHandler(ContributorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContributorNotFound(ContributorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Bad Request", "Некорректное тело запроса"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .sorted()
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Validation Failed",
                "Request validation failed", details));
    }

    /**
     * Spring 6.1+ бросает NoResourceFoundException вместо NoHandlerFoundException
     * для несуществующих URL. Без явного обработчика catch-all вернул бы 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Not Found",
                "No endpoint at: " + ex.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new ErrorResponse(405, "Method Not Allowed", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "Internal Server Error",
                "An unexpected error occurred"));
    }
}
