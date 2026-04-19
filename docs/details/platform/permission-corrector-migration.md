---
status: in-progress
module: permission
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolver.java
---

# Row-Level Permission Corrector Migration

## Objective
Move data row/column permission from `S2DataPermissionAspect` (AOP on `@S2DataPermission` methods) to the physical-SQL corrector chain.

## Timeline
| Phase | Flag | Action |
|-------|------|--------|
| 1. Ship (T0) | `shadow-mode=true` | Both run; diff logs WARN |
| 2. Monitor (T0 → T+30d) | | Count WARN lines daily; target = 0 for 7 consecutive days |
| 3. Flip (T+30d) | `shadow-mode=false` | New corrector rewrites; aspect still runs for safety |
| 4. Remove (T+60d) | — | Delete `S2DataPermissionAspect`, remove `@S2DataPermission` annotation, drop `needAuth` coupling |

## Kill-switch
`s2.permission.corrector.enabled=false` disables the whole chain — use if shadow-mode reports a production-breaking diff. Emergencies only.

## Audit log format
JSON line on logger `s2.permission.audit`:
```json
{"ts":"2026-04-17T10:00:00Z","policyId":"P1","user":"alice","policyType":"row","sqlDigest":"sha256:abc"}
```

## Known diff causes (rule out before flipping)
1. Aspect injects `(a OR b)` joined by `OR`; new corrector AND-joins per policy. If you want OR-within-policy, encode it in `filterExpression` itself.
2. Aspect wraps all existing WHERE in parens before adding; new corrector relies on JSqlParser re-serialisation. Whitespace normalisation handles this — if not, bug.
3. `SELECT *` is masked by aspect via post-result-set column drop; new corrector no-ops. This is a **known degradation** documented here. Fix: require explicit columns in LLM prompt, or add a projection rewriter in a later iteration.
