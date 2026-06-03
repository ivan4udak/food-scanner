CREATE TABLE food_catalog.catalog_entry_photos (
    id          UUID          NOT NULL,
    entry_id    UUID          NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_catalog_entry_photos PRIMARY KEY (id),
    CONSTRAINT fk_catalog_entry_photos_entry
        FOREIGN KEY (entry_id)
        REFERENCES food_catalog.catalog_entries (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_catalog_entry_photos_entry_id
    ON food_catalog.catalog_entry_photos (entry_id);
