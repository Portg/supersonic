CREATE TABLE IF NOT EXISTS s2_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    event_type VARCHAR(200) NOT NULL,
    payload_json CLOB NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP(3),
    processing_node VARCHAR(100),
    attempts INT NOT NULL DEFAULT 0,
    last_error CLOB
);
CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    event_type VARCHAR(200) NOT NULL,
    payload_json CLOB NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    failure_reason CLOB NOT NULL,
    attempts INT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    died_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
