CREATE TABLE food_catalog.catalog_drafts (
    id              UUID        NOT NULL,
    barcode         VARCHAR(50) NOT NULL,
    contributor_id  UUID        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_catalog_drafts PRIMARY KEY (id),
    CONSTRAINT fk_catalog_drafts_contributor
        FOREIGN KEY (contributor_id)
        REFERENCES food_catalog.contributors (id)
);

CREATE INDEX idx_catalog_drafts_barcode_status
    ON food_catalog.catalog_drafts (barcode, status);

CREATE INDEX idx_catalog_drafts_contributor_id
    ON food_catalog.catalog_drafts (contributor_id);
