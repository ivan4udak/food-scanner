CREATE TABLE food_catalog.catalog_entries (
    id              UUID        NOT NULL,
    barcode         VARCHAR(50) NOT NULL,
    contributor_id  UUID        NOT NULL,
    draft_id        UUID        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_catalog_entries PRIMARY KEY (id),
    CONSTRAINT uq_catalog_entries_barcode  UNIQUE (barcode),
    CONSTRAINT uq_catalog_entries_draft_id UNIQUE (draft_id),
    CONSTRAINT fk_catalog_entries_contributor
        FOREIGN KEY (contributor_id)
        REFERENCES food_catalog.contributors (id),
    CONSTRAINT fk_catalog_entries_draft
        FOREIGN KEY (draft_id)
        REFERENCES food_catalog.catalog_drafts (id)
);
