INSERT IGNORE INTO s2_llm_pricing
    (provider, model, input_price_per_1k_micros, output_price_per_1k_micros, currency, effective_from)
VALUES
    ('OPEN_AI', 'gpt-4o-mini', 150, 600, 'USD', '2026-01-01 00:00:00'),
    ('OPEN_AI', 'gpt-4o', 2500, 10000, 'USD', '2026-01-01 00:00:00'),
    ('OPEN_AI', 'gpt-4.1-mini', 400, 1600, 'USD', '2026-01-01 00:00:00'),
    ('OPEN_AI', 'gpt-4.1', 2000, 8000, 'USD', '2026-01-01 00:00:00'),
    ('DIFY', 'default-zero-cost-placeholder', 0, 0, 'USD', '2026-01-01 00:00:00'),
    ('OLLAMA', 'qwen:0.5b', 0, 0, 'USD', '2026-01-01 00:00:00'),
    ('LOCAL_AI', 'ggml-gpt4all-j', 0, 0, 'USD', '2026-01-01 00:00:00');
