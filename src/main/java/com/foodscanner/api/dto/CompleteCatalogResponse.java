package com.foodscanner.api.dto;

import java.util.UUID;

public class CompleteCatalogResponse {
    private UUID catalogEntryId;
    private int  contributorCompletedCount;

    public CompleteCatalogResponse() {}
    public CompleteCatalogResponse(UUID catalogEntryId, int contributorCompletedCount) {
        this.catalogEntryId            = catalogEntryId;
        this.contributorCompletedCount = contributorCompletedCount;
    }

    public UUID getCatalogEntryId()                       { return catalogEntryId; }
    public int  getContributorCompletedCount()            { return contributorCompletedCount; }
    public void setCatalogEntryId(UUID v)                 { this.catalogEntryId = v; }
    public void setContributorCompletedCount(int v)       { this.contributorCompletedCount = v; }
}
