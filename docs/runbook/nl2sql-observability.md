# NL2SQL Observability Runbook

Dashboard: Grafana → `SuperSonic NL2SQL Chain Observability` (uid `s2-nl2sql`).
Alert rules: `docker/prometheus/rules/nl2sql-slo-alert-rules.yml`.

## Metric cheatsheet

| Metric | Type | Tags | Meaning |
|---|---|---|---|
| `s2_nl2sql_stage_duration_seconds` | histogram | stage, outcome, tenant_id, agent_id, parser_name | per-stage latency (rule_parse, llm_parse, mapper, corrector) |
| `s2_nl2sql_stage_outcome_total` | counter | stage, outcome | outcome breakdown |
| `s2_nl2sql_llm_duration_seconds` | histogram | model, outcome, tenant_id | LLM call latency |
| `s2_nl2sql_llm_tokens_total` | counter | model, kind (prompt/completion) | token usage |
| `s2_nl2sql_mapper_hits_total` | counter | mapper_name, hit (true/false) | mapper effectiveness |
| `s2_nl2sql_db_duration_seconds` | histogram | db_type, outcome | DB execution latency |
| `s2_nl2sql_sql_rows_scanned` | summary | db_type | rows scanned per SQL |

## Alert response — `Nl2sqlStageP99High`

1. Open the dashboard, filter to the alerting `stage`.
2. Identify whether one tenant dominates. Switch `tenant` template var.
3. Tail logs: `jq 'select(.queryTraceId)' logs/s2-json.log | tail -n 200`.
4. If `stage=llm_parse`: check `s2_nl2sql_llm_duration_seconds` panel — if only one model is slow, consider failover. If all models, check LLM provider status page.
5. If `stage=mapper`: check HanLP / embedding backends (Milvus / Chroma).
6. If `stage=corrector`: find the corrector by `corrector_name` tag. Roll back the last corrector change if timing regressed.

## Alert response — `Nl2sqlErrorRateHigh`

1. Filter dashboard by `outcome=error`.
2. Query recent traces: `jq 'select(.level=="ERROR" and .queryTraceId)' logs/s2-json.log | head -n 50`.
3. Pick one `queryTraceId` and `jq 'select(.queryTraceId=="q_...")' logs/s2-json.log` for the full timeline.
4. Common root causes: schema drift after deployment, LLM API quota exceeded, tenant DB outage.

## Trace propagation

- `queryTraceId` is set at `NL2SQLParser#parse` entry (`QueryTraceContext.open()`).
- It flows through MDC into every log line. Async boundaries are covered by `ContextAwareThreadPoolExecutor` (already in use).
- For new async code, wrap with `QueryTraceContext.wrap(Runnable)` or `snapshot()` / `restore()`.

## Cardinality guardrails

- `tenant_id` is capped at 50 unique values per process via `TenantTagNormalizer`. Tenants outside the configured `s2.observability.nl2sql.top-tenants` allowlist plus dynamic admission get mapped to `tenant_id=other`.
- Raw query text is NEVER emitted as a tag — only aggregated metrics and the `queryTraceId` (in logs, not metrics).
- If new tags are proposed, stop and check: does the cardinality stay < 200 per metric? If not, use log-aggregation (Loki) instead.

## Configuration

`launchers/standalone/src/main/resources/application.yaml`:
```yaml
s2:
  observability:
    nl2sql:
      tenant-tag-limit: 50          # max unique tenant_id values
      top-tenants: []               # explicit allowlist; populate via ops config
      emit-tenant-tag: true         # set false for global aggregate view only
```

## Validation after deploy

1. Hit a couple of NL2SQL queries through the chat UI.
2. `curl -s http://<host>/actuator/prometheus | grep s2_nl2sql` — confirm at least one `_count` > 0 for each metric family.
3. Open the dashboard — all panels should render data within 2 min.
4. Trigger a synthetic error (e.g., disabled LLM) and verify `Nl2sqlErrorRateHigh` fires within 10 min.
