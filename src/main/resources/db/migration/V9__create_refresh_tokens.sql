CREATE TABLE food_catalog.refresh_tokens (
    id             UUID         NOT NULL,
    contributor_id UUID         NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_contributor
        FOREIGN KEY (contributor_id)
        REFERENCES food_catalog.contributors (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_contributor ON food_catalog.refresh_tokens (contributor_id);
