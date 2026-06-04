package com.foodscanner.application.command;

public final class RegisterAccountCommand {
    private final String username;
    private final String password;

    public RegisterAccountCommand(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
