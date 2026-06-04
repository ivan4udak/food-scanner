-- Контент-адресное хранилище: дедупликация фото по SHA-256 (Блок 16).
-- hash → object_key в MinIO. Несколько draft_photos/entry_photos могут ссылаться
-- на один object_key (общий контент).
CREATE TABLE food_catalog.photo_objects (
    hash       VARCHAR(64)   NOT NULL,
    object_key VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_photo_objects PRIMARY KEY (hash)
);

-- Индекс по object_key — для проверки/очистки ссылок.
CREATE INDEX idx_photo_objects_object_key ON food_catalog.photo_objects (object_key);
