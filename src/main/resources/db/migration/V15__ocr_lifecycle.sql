-- v1.10.4 — OCR lifecycle hardening: актуальность задачи, orphan, трекинг публикации.
-- Статусы 0–5 НЕ трогаем (см. docs/OCR.md); добавляем отдельные поля жизненного цикла.
ALTER TABLE food_catalog.ocr_jobs
    ADD COLUMN active             BOOLEAN     NOT NULL DEFAULT TRUE,   -- последняя задача для (draft, photo_type)
    ADD COLUMN superseded_at      TIMESTAMPTZ,
    ADD COLUMN superseded_by      UUID,
    ADD COLUMN orphaned           BOOLEAN     NOT NULL DEFAULT FALSE,  -- черновик/фото исчезли
    ADD COLUMN orphaned_at        TIMESTAMPTZ,
    ADD COLUMN published_at       TIMESTAMPTZ,                          -- успешно опубликовано в RabbitMQ
    ADD COLUMN publish_attempts   INT         NOT NULL DEFAULT 0,
    ADD COLUMN last_publish_error TEXT;

-- актуальная выборка (active=true AND orphaned=false) и поиск по (draft, photo_type)
CREATE INDEX idx_ocr_jobs_active      ON food_catalog.ocr_jobs (active, orphaned);
CREATE INDEX idx_ocr_jobs_draft_type  ON food_catalog.ocr_jobs (draft_id, photo_type);
-- неопубликованные активные QUEUED — для republish
CREATE INDEX idx_ocr_jobs_republish   ON food_catalog.ocr_jobs (status, published_at) WHERE active;
