-- v1.11.3 — поля структурного извлечения продукта (фундамент под LLM-этап).
-- composition → parsed_ingredients (уже есть), КБЖУ → parsed_nutrition (jsonb, уже есть).
-- Здесь добавляем название/бренд/производителя. Все nullable, заполнит будущий ProductExtractor.
ALTER TABLE food_catalog.ocr_jobs
    ADD COLUMN parsed_name         TEXT,
    ADD COLUMN parsed_brand        TEXT,
    ADD COLUMN parsed_manufacturer TEXT;
