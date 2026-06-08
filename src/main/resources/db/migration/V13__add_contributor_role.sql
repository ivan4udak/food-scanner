-- v1.8.0 — роли участников для админ-панели.
ALTER TABLE food_catalog.contributors
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER';
