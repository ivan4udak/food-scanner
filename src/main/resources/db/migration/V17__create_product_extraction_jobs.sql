-- v1.12.0 — foundation: очередь задач структурного извлечения продукта (отдельно от OCR).
-- OCR достаёт сырой текст; Product Extraction понимает текст/фото и извлекает поля.
-- Источник: TEXT_EXTRACTION (по rawText) либо IMAGE_FALLBACK_EXTRACTION (по фото, если OCR пуст/плох).
-- Обработка — медленная, ночным батчем (worker/окно — следующий срез). Здесь только данные.
CREATE TABLE food_catalog.product_extraction_jobs (
    id            UUID         NOT NULL,
    ocr_job_id    UUID         NOT NULL,                 -- источник (FK-less, как ocr_jobs)
    barcode       VARCHAR(64),
    type          VARCHAR(32)  NOT NULL,                 -- TEXT_EXTRACTION | IMAGE_FALLBACK_EXTRACTION
    status        SMALLINT     NOT NULL DEFAULT 0,       -- 0 QUEUED..5 SKIPPED (см. docs/OCR.md)
    attempts      INT          NOT NULL DEFAULT 0,
    -- структурный результат (заполняет worker+extractor в след. срезах; пока null)
    source        VARCHAR(32),
    name          TEXT,
    brand         TEXT,
    manufacturer  TEXT,
    composition   TEXT,
    nutrition     JSONB,
    confidence    JSONB,
    needs_review  BOOLEAN,
    last_error    TEXT,
    queued_at     TIMESTAMPTZ  NOT NULL,
    processed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_product_extraction_jobs PRIMARY KEY (id)
);

CREATE INDEX idx_pej_status   ON food_catalog.product_extraction_jobs (status);
CREATE INDEX idx_pej_ocr_job  ON food_catalog.product_extraction_jobs (ocr_job_id);
CREATE INDEX idx_pej_queued   ON food_catalog.product_extraction_jobs (status, queued_at);
