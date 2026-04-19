---
status: stable
module: permission
key-files:
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/RowPolicy.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/ColumnPolicy.java
---

# Authoring Row / Column Policies

## Row policy cheat-sheet

A `RowPolicy` maps (user, model) → a SQL boolean expression injected into `WHERE` of every `PlainSelect` that touches a listed `tableBizName`. Walk happens on JSqlParser AST, so the filter survives UNION, CTE, nested sub-queries.

| Field | Type | Notes |
|---|---|---|
| `policyId` | String | Unique ID (UUID or human-readable) |
| `modelId` | Long | SuperSonic model ID |
| `tableBizNames` | List\<String\> | Physical table names to match (case-insensitive) |
| `filterExpression` | String | Valid SQL boolean e.g. `region = 'APAC'` |
| `description` | String | Human-readable label for audit logs |

### Safe filter expression patterns
```sql
-- simple equality
department = 'finance'
-- IN-list
status IN ('ACTIVE', 'PENDING')
-- composite AND
region = 'APAC' AND tier = 'gold'
```

### Unsafe patterns (will break or silently no-op)
```sql
-- subqueries in filters — parser may reject or produce wrong AST
user_id IN (SELECT id FROM acl_table WHERE ...)
-- unbalanced parens — JSqlParser rejects
(region = 'APAC'
```

## Column policy cheat-sheet

A `ColumnPolicy` wraps a named column in a SQL expression template. Applied at `SelectItem` level on the outermost `PlainSelect`.

| Field | Type | Notes |
|---|---|---|
| `policyId` | String | Unique ID |
| `modelId` | Long | SuperSonic model ID |
| `columnBizName` | String | Logical column name in the SELECT (case-insensitive) |
| `maskTemplate` | String | `String.format`-style with one `%s` for the column reference |

### Common mask templates
| Use case | Template |
|---|---|
| Show first 3 chars | `CONCAT(LEFT(%s,3),'****')` |
| Full redact | `'[REDACTED]'` |
| Hash | `MD5(%s)` |

### Limitations
- `SELECT *` cannot be surgically masked — avoid `SELECT *` queries in sensitive datasets, or expand to explicit column list at the LLM-prompt level.
- Masking applies to the outermost `PlainSelect` only. If a CTE or subquery projects the sensitive column and the outer query selects it by alias, the mask will not apply.

## Reviewer checklist

Before merging a new policy configuration:

- [ ] `filterExpression` parses as valid SQL boolean (test with JSqlParser or against a real DB)
- [ ] `tableBizNames` match actual physical table names used by the model (check model definition)
- [ ] Policy is least-privilege: denies exactly what is needed, no more
- [ ] `maskTemplate` uses `%s` exactly once and produces a valid SQL expression
- [ ] Policy is covered by a golden-fixture test case in `permission-fixtures/golden-rewrites.json`
- [ ] Audit logger name `s2.permission.audit` is configured in ops log routing

## Testing policies locally

Add a fixture to `headless/core/src/test/resources/permission-fixtures/golden-rewrites.json`:

```json
{
  "name": "my_new_policy",
  "user": "alice",
  "rowPolicies": [{"policyId": "P99", "modelId": 1, "tableBizNames": ["my_table"], "filterExpression": "dept = 'eng'"}],
  "columnPolicies": [],
  "inputSql": "SELECT id, name FROM my_table WHERE created > '2026-01-01'",
  "expectedSql": "SELECT id, name FROM my_table WHERE created > '2026-01-01' AND (dept = 'eng')"
}
```

Run: `mvn test -pl headless/core -Dtest=GoldenRewriteFixtureTest`
