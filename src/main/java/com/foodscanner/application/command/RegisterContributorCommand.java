package com.foodscanner.application.command;

public final class RegisterContributorCommand {
    private final String nickname;
    public RegisterContributorCommand(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }
}
