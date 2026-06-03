package com.foodscanner.application.command;

import java.util.UUID;

public final class CompleteCatalogCommand {
    private final UUID draftId;
    private final UUID contributorId;

    public CompleteCatalogCommand(UUID draftId, UUID contributorId) {
        this.draftId       = draftId;
        this.contributorId = contributorId;
    }

    public UUID getDraftId()       { return draftId; }
    public UUID getContributorId() { return contributorId; }
}
