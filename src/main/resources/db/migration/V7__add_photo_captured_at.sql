-- Дата/время съёмки фотографии (из метаданных галереи).
-- Nullable: для снимков с камеры или фото без EXIF может отсутствовать.
ALTER TABLE food_catalog.draft_photos
    ADD COLUMN captured_at TIMESTAMPTZ NULL;

ALTER TABLE food_catalog.catalog_entry_photos
    ADD COLUMN captured_at TIMESTAMPTZ NULL;
