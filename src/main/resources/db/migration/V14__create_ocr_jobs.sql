-- v1.10.0 — OCR job lifecycle (foundation). Заполняется при загрузке INGREDIENTS/NUTRITION.
CREATE TABLE food_catalog.ocr_jobs (
    id                  UUID         NOT NULL,
    draft_id            UUID,
    catalog_entry_id    UUID,
    storage_key         VARCHAR(1000) NOT NULL,
    photo_type          VARCHAR(20)  NOT NULL,
    status              SMALLINT     NOT NULL DEFAULT 0,  -- 0..5 (см. docs/OCR.md)
    attempts            INT          NOT NULL DEFAULT 0,
    raw_text            TEXT,
    parsed_ingredients  TEXT,
    parsed_nutrition    JSONB,
    confidence          DOUBLE PRECISION,
    error_code          VARCHAR(128),
    error_message       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ocr_jobs PRIMARY KEY (id)
);

CREATE INDEX idx_ocr_jobs_draft_id   ON food_catalog.ocr_jobs (draft_id);
CREATE INDEX idx_ocr_jobs_entry_id   ON food_catalog.ocr_jobs (catalog_entry_id);
CREATE INDEX idx_ocr_jobs_storage    ON food_catalog.ocr_jobs (storage_key);
CREATE INDEX idx_ocr_jobs_status     ON food_catalog.ocr_jobs (status);
