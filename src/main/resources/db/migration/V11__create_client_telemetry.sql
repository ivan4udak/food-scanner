-- v1.7.0 — серверное хранение клиентской телеметрии и бизнес-событий.
-- Аддитивно: новые таблицы, существующие не трогаем.

-- ── Клиентские логи ──────────────────────────────────────────────────
CREATE TABLE food_catalog.client_logs (
    id                UUID         NOT NULL,
    contributor_id    UUID         NOT NULL,
    session_id        UUID         NOT NULL,
    client_log_id     VARCHAR(128),
    correlation_id    UUID,
    request_id        UUID,
    "timestamp"       TIMESTAMPTZ  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL,
    level             VARCHAR(16)  NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    event             VARCHAR(128),
    message           TEXT,
    screen            VARCHAR(128),
    metadata_json     JSONB,
    duration_ms       BIGINT,
    stack_trace       TEXT,
    barcode           VARCHAR(64),
    draft_id          UUID,
    catalog_entry_id  UUID,
    photo_id          UUID,
    api_method        VARCHAR(16),
    api_path          VARCHAR(256),
    http_status       INT,
    client_version    VARCHAR(64),
    pwa_version       VARCHAR(64),
    CONSTRAINT pk_client_logs PRIMARY KEY (id)
);

CREATE INDEX idx_client_logs_contributor_id ON food_catalog.client_logs (contributor_id);
CREATE INDEX idx_client_logs_session_id     ON food_catalog.client_logs (session_id);
CREATE INDEX idx_client_logs_timestamp      ON food_catalog.client_logs ("timestamp");
CREATE INDEX idx_client_logs_level          ON food_catalog.client_logs (level);
CREATE INDEX idx_client_logs_category       ON food_catalog.client_logs (category);
CREATE INDEX idx_client_logs_event          ON food_catalog.client_logs (event);
CREATE INDEX idx_client_logs_correlation_id ON food_catalog.client_logs (correlation_id);
CREATE INDEX idx_client_logs_request_id     ON food_catalog.client_logs (request_id);
CREATE INDEX idx_client_logs_barcode        ON food_catalog.client_logs (barcode);
CREATE INDEX idx_client_logs_draft_id       ON food_catalog.client_logs (draft_id);

-- ── Клиентские сессии (одна строка на sessionId, обновляется last_seen) ──
CREATE TABLE food_catalog.client_sessions (
    id                    UUID              NOT NULL,
    contributor_id        UUID              NOT NULL,
    session_id            UUID              NOT NULL,
    started_at            TIMESTAMPTZ       NOT NULL,
    last_seen_at          TIMESTAMPTZ       NOT NULL,
    client_version        VARCHAR(64),
    pwa_version           VARCHAR(64),
    browser               VARCHAR(128),
    os                    VARCHAR(128),
    device_type           VARCHAR(64),
    language              VARCHAR(32),
    timezone              VARCHAR(128),
    screen_width          INT,
    screen_height         INT,
    hardware_concurrency  INT,
    device_memory         DOUBLE PRECISION,
    network_status        VARCHAR(32),
    standalone            BOOLEAN,
    CONSTRAINT pk_client_sessions PRIMARY KEY (id),
    CONSTRAINT uq_client_sessions_session_id UNIQUE (session_id)
);

CREATE INDEX idx_client_sessions_contributor_id ON food_catalog.client_sessions (contributor_id);
CREATE INDEX idx_client_sessions_last_seen_at   ON food_catalog.client_sessions (last_seen_at);

-- ── Активность клиента (лёгкие heartbeat-события для online/last-activity) ──
CREATE TABLE food_catalog.client_activity (
    id              UUID         NOT NULL,
    contributor_id  UUID         NOT NULL,
    session_id      UUID         NOT NULL,
    screen          VARCHAR(128),
    online          BOOLEAN,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    received_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_client_activity PRIMARY KEY (id)
);

CREATE INDEX idx_client_activity_contributor_id ON food_catalog.client_activity (contributor_id);
CREATE INDEX idx_client_activity_session_id     ON food_catalog.client_activity (session_id);
CREATE INDEX idx_client_activity_occurred_at    ON food_catalog.client_activity (occurred_at);

-- ── Серверные бизнес-события (не весь шум, а значимые события) ──
CREATE TABLE food_catalog.server_events (
    id                UUID         NOT NULL,
    occurred_at       TIMESTAMPTZ  NOT NULL,
    level             VARCHAR(16)  NOT NULL,
    event             VARCHAR(128) NOT NULL,
    correlation_id    UUID,
    request_id        UUID,
    contributor_id    UUID,
    username          VARCHAR(128),
    session_id        UUID,
    method            VARCHAR(16),
    path              VARCHAR(256),
    http_status       INT,
    duration_ms       BIGINT,
    use_case          VARCHAR(128),
    barcode           VARCHAR(64),
    draft_id          UUID,
    catalog_entry_id  UUID,
    photo_id          UUID,
    error_code        VARCHAR(128),
    error_message     TEXT,
    exception_class   VARCHAR(256),
    metadata_json     JSONB,
    CONSTRAINT pk_server_events PRIMARY KEY (id)
);

CREATE INDEX idx_server_events_correlation_id ON food_catalog.server_events (correlation_id);
CREATE INDEX idx_server_events_request_id     ON food_catalog.server_events (request_id);
CREATE INDEX idx_server_events_contributor_id ON food_catalog.server_events (contributor_id);
CREATE INDEX idx_server_events_barcode        ON food_catalog.server_events (barcode);
CREATE INDEX idx_server_events_event          ON food_catalog.server_events (event);
CREATE INDEX idx_server_events_occurred_at    ON food_catalog.server_events (occurred_at);
