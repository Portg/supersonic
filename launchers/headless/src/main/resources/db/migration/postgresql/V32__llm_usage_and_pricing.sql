CREATE TABLE IF NOT EXISTS s2_llm_usage (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL DEFAULT 1,
    user_id                 VARCHAR(100),
    model                   VARCHAR(200) NOT NULL,
    provider                VARCHAR(50)  NOT NULL,
    call_type               VARCHAR(50)  NOT NULL,
    input_tokens            INT          NOT NULL DEFAULT 0,
    output_tokens           INT          NOT NULL DEFAULT 0,
    total_tokens            INT          NOT NULL DEFAULT 0,
    estimated_cost_micros   BIGINT       NOT NULL DEFAULT 0,
    request_id              VARCHAR(64),
    trace_id                VARCHAR(64),
    latency_ms              INT,
    success                 SMALLINT     NOT NULL DEFAULT 1,
    error_type              VARCHAR(100),
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_llm_usage_tenant_date ON s2_llm_usage (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_llm_usage_model ON s2_llm_usage (model);
CREATE INDEX IF NOT EXISTS idx_llm_usage_call_type ON s2_llm_usage (call_type);
CREATE INDEX IF NOT EXISTS idx_llm_usage_request_id ON s2_llm_usage (request_id);

CREATE TABLE IF NOT EXISTS s2_llm_pricing (
    id                      BIGSERIAL    PRIMARY KEY,
    provider                VARCHAR(50)  NOT NULL,
    model                   VARCHAR(200) NOT NULL,
    input_price_per_1k_micros  BIGINT    NOT NULL DEFAULT 0,
    output_price_per_1k_micros BIGINT    NOT NULL DEFAULT 0,
    currency                VARCHAR(10)  NOT NULL DEFAULT 'USD',
    effective_from          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pricing_provider_model UNIQUE (provider, model, effective_from)
);
