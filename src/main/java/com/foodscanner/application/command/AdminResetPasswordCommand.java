package com.foodscanner.application.command;

public final class AdminResetPasswordCommand {
    private final String role;
    private final String password;
    private final String username;

    public AdminResetPasswordCommand(String role, String password, String username) {
        this.role     = role;
        this.password = password;
        this.username = username;
    }

    public String getRole()     { return role; }
    public String getPassword() { return password; }
    public String getUsername() { return username; }
}
