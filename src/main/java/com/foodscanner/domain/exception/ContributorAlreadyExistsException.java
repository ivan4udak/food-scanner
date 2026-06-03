package com.foodscanner.domain.exception;

public class ContributorAlreadyExistsException extends RuntimeException {
    public ContributorAlreadyExistsException(String nickname) {
        super("Contributor with nickname '" + nickname + "' already exists");
    }
}
