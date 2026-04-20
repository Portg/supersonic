CREATE TABLE IF NOT EXISTS s2_llm_usage (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL DEFAULT 1,
    user_id                 VARCHAR(100),
    model                   VARCHAR(200) NOT NULL,
    provider                VARCHAR(50)  NOT NULL,
    call_type               VARCHAR(50)  NOT NULL COMMENT 'NL2SQL, SUMMARY, PLUGIN, DATA_INTERPRET, CORRECTOR, MAPPER, ALIAS, MEMORY_REVIEW, UNKNOWN',
    input_tokens            INT          NOT NULL DEFAULT 0,
    output_tokens           INT          NOT NULL DEFAULT 0,
    total_tokens            INT          NOT NULL DEFAULT 0,
    estimated_cost_micros   BIGINT       NOT NULL DEFAULT 0 COMMENT 'USD cost in micro-dollars (1e-6); 0 if pricing unknown',
    request_id              VARCHAR(64),
    trace_id                VARCHAR(64),
    latency_ms              INT,
    success                 TINYINT(1)   NOT NULL DEFAULT 1,
    error_type              VARCHAR(100),
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_llm_usage_tenant_date (tenant_id, created_at),
    INDEX idx_llm_usage_model (model),
    INDEX idx_llm_usage_call_type (call_type),
    INDEX idx_llm_usage_request_id (request_id)
);

CREATE TABLE IF NOT EXISTS s2_llm_pricing (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider                VARCHAR(50)  NOT NULL,
    model                   VARCHAR(200) NOT NULL,
    input_price_per_1k_micros  BIGINT    NOT NULL DEFAULT 0 COMMENT 'Cost per 1k input tokens in micro-USD',
    output_price_per_1k_micros BIGINT    NOT NULL DEFAULT 0 COMMENT 'Cost per 1k output tokens in micro-USD',
    currency                VARCHAR(10)  NOT NULL DEFAULT 'USD',
    effective_from          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to            DATETIME,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pricing_provider_model (provider, model, effective_from)
);
