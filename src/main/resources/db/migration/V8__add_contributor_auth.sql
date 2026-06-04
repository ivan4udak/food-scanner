-- Аутентификация контрибьюторов (vNext).
-- username/password_hash nullable: legacy-записи (регистрация по нику) их не имеют.
ALTER TABLE food_catalog.contributors
    ADD COLUMN username              VARCHAR(100),
    ADD COLUMN password_hash         VARCHAR(100),
    ADD COLUMN failed_login_attempts INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMPTZ,
    ADD COLUMN reset_password_until  TIMESTAMPTZ;

-- Логин уникален среди заданных (несколько NULL допустимы в Postgres).
ALTER TABLE food_catalog.contributors
    ADD CONSTRAINT uq_contributors_username UNIQUE (username);
