package com.foodscanner.application.result;

import java.util.UUID;

/** Результат создания аккаунта или восстановления пароля. */
public final class AccountResult {
    private final UUID   contributorId;
    private final String username;

    public AccountResult(UUID contributorId, String username) {
        this.contributorId = contributorId;
        this.username      = username;
    }

    public UUID   getContributorId() { return contributorId; }
    public String getUsername()      { return username; }
}
