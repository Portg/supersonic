-- V34__outbox.sql
-- Transactional outbox for cross-module ApplicationEvents.

CREATE TABLE IF NOT EXISTS s2_outbox (
    id               BIGSERIAL PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     TEXT         NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at     TIMESTAMP(3),
    processing_node  VARCHAR(100),
    attempts         INT          NOT NULL DEFAULT 0,
    last_error       TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed ON s2_outbox (processed_at, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant      ON s2_outbox (tenant_id);

CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id               BIGSERIAL PRIMARY KEY,
    original_id      BIGINT       NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     TEXT         NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    failure_reason   TEXT         NOT NULL,
    attempts         INT          NOT NULL,
    created_at       TIMESTAMP(3) NOT NULL,
    died_at          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_dead_tenant ON s2_outbox_dead (tenant_id);
