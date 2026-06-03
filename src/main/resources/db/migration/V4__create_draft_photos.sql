CREATE TABLE food_catalog.draft_photos (
    id          UUID          NOT NULL,
    draft_id    UUID          NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_draft_photos PRIMARY KEY (id),
    CONSTRAINT fk_draft_photos_draft
        FOREIGN KEY (draft_id)
        REFERENCES food_catalog.catalog_drafts (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_draft_photos_draft_id
    ON food_catalog.draft_photos (draft_id);
