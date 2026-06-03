package com.foodscanner.domain.exception;

/** Контрибьютор не найден. */
public class ContributorNotFoundException extends RuntimeException {
    public ContributorNotFoundException(String username) {
        super("Contributor not found: " + username);
    }
}
