CREATE TABLE food_catalog.contributors (
    id          UUID         NOT NULL,
    nickname    VARCHAR(100) NOT NULL,
    completed_catalog_count INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_contributors PRIMARY KEY (id),
    CONSTRAINT uq_contributors_nickname UNIQUE (nickname)
);
