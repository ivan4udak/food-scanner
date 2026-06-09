-- v1.7.0 — приватность публичного рейтинга: пользователь может скрыть себя.
ALTER TABLE food_catalog.contributors
    ADD COLUMN hidden_from_leaderboard BOOLEAN NOT NULL DEFAULT FALSE;
