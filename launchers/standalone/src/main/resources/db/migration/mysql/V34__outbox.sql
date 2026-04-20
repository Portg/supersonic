-- V34__outbox.sql
-- Transactional outbox for cross-module ApplicationEvents.
-- NOTE: MySQL does NOT support ADD COLUMN IF NOT EXISTS. These are fresh CREATE TABLE only.

CREATE TABLE IF NOT EXISTS s2_outbox (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL COMMENT 'e.g. SemanticTemplate, Plugin, Model',
    aggregate_id     VARCHAR(100)          COMMENT 'business id (nullable for events without one)',
    event_type       VARCHAR(200) NOT NULL COMMENT 'fully qualified class name',
    payload_json     MEDIUMTEXT   NOT NULL COMMENT 'Jackson-serialized event (source field omitted)',
    tenant_id        BIGINT       DEFAULT 1,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at     DATETIME(3)  NULL,
    processing_node  VARCHAR(100) NULL     COMMENT 'host:pid of relay that is currently holding lock',
    attempts         INT          NOT NULL DEFAULT 0,
    last_error       TEXT         NULL,
    INDEX idx_outbox_unprocessed (processed_at, created_at),
    INDEX idx_outbox_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_id      BIGINT       NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     MEDIUMTEXT   NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    failure_reason   TEXT         NOT NULL,
    attempts         INT          NOT NULL,
    created_at       DATETIME(3)  NOT NULL,
    died_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_outbox_dead_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
