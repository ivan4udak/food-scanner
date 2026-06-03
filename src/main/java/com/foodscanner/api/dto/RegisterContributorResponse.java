package com.foodscanner.api.dto;

import java.util.UUID;

public class RegisterContributorResponse {
    private UUID   contributorId;
    private String nickname;

    public RegisterContributorResponse() {}
    public RegisterContributorResponse(UUID contributorId, String nickname) {
        this.contributorId = contributorId;
        this.nickname      = nickname;
    }

    public UUID   getContributorId()              { return contributorId; }
    public String getNickname()                   { return nickname; }
    public void   setContributorId(UUID v)        { this.contributorId = v; }
    public void   setNickname(String v)           { this.nickname = v; }
}
