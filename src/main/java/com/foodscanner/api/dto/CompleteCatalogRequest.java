package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CompleteCatalogRequest {

    @NotNull(message = "ContributorId must not be null")
    private UUID contributorId;

    public CompleteCatalogRequest() {}
    public CompleteCatalogRequest(UUID contributorId) { this.contributorId = contributorId; }

    public UUID getContributorId()          { return contributorId; }
    public void setContributorId(UUID v)    { this.contributorId = v; }
}
