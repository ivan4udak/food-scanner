package com.foodscanner.application.command;

public final class RecoverPasswordCommand {
    private final String username;
    private final String newPassword;

    public RecoverPasswordCommand(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }

    public String getUsername()    { return username; }
    public String getNewPassword() { return newPassword; }
}
