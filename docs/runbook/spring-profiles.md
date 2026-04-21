# Spring Profile Runbook — SuperSonic (`launchers/standalone`)

## Activation model

Two independent axes, both driven by env var:

| Env var      | Spring key                | Values (defaults in parens)            | Role                                                                    |
|--------------|---------------------------|----------------------------------------|-------------------------------------------------------------------------|
| `S2_DB_TYPE` | `spring.profiles.active`  | `h2` (default) / `mysql` / `postgres`  | Database dialect — which `application-<db>.yaml` gets loaded            |
| `S2_ENV`     | `spring.profiles.include` | `dev` (default) / `prd`               | Environment — which `application-<env>.yaml` gets loaded (credentials)  |

The two axes are **independent**: production can run on either MySQL or Postgres; a developer on `dev` can still pick either DB for local testing.

Spring's resolution order (later wins):
1. `application.yaml` (base — always loaded)
2. `application-<active>.yaml` (DB dialect)
3. `application-<included>.yaml` (env)
4. Env vars
5. JVM `-D` system properties

## File ownership rule

- **`application.yaml`** = every setting that is identical across every `(db, env)` combination.
- **`application-<db>.yaml`** = only what differs per DB dialect (JDBC URL/driver, Flyway `locations`, `validate-on-migrate`, pgvector for Postgres, H2 console for H2).
- **`application-<env>.yaml`** = credentials, per-env flags, optional SMTP overrides.

Enforced automatically by `ProfileYamlDiffTest` (fails CI if a profile yaml grows keys outside its allow-list).

## How to add a new env profile (e.g. `staging`)

1. Create `launchers/standalone/src/main/resources/application-staging.yaml`.
2. Put ONLY credentials/env-specific overrides — mirror `application-prd.yaml` shape.
3. Add the file to `ALLOWED_PREFIXES` in `ProfileYamlDiffTest` with the allowed prefix list (typically the same as prd).
4. Add a `@Nested` class to `ResolvedPropertyParityTest` with `@ActiveProfiles({"<db>", "staging"})` asserting critical resolved values.
5. Activate in deployment via `S2_ENV=staging`.
6. Update `docker/.env.example` if the new profile should be documented there.
7. If the new env needs a credential template committed to the repo, create `application-staging.yaml.example` alongside and add the real `application-staging.yaml` to `.gitignore` (mirror the dev pattern).

## How to add a new DB dialect (e.g. `clickhouse`)

1. Create `launchers/standalone/src/main/resources/application-clickhouse.yaml` with JDBC + Flyway overrides.
2. Add `classpath:db/migration/clickhouse/` Flyway scripts if applicable, else set `spring.flyway.enabled: false`.
3. Add to `ALLOWED_PREFIXES` in `ProfileYamlDiffTest`.
4. Add a nested parity test.
5. Add the JDBC driver to `launchers/standalone/pom.xml`.

## Known gotchas

- **Docker-compose defaults**: `docker/docker-compose.yml` pins `S2_DB_TYPE=postgres` and (since P2-11) `S2_ENV=prd`. Override by adding envs to the compose file or `.env`.
- **`application-dev.yaml` is gitignored.** Use `application-dev.yaml.example` as the template. `git pull` will never overwrite your local dev secrets.
- **Prd mail config**: base `application.yaml` reads `${EMAIL_HOST}` with no default. If prd should have a fallback, set it via env var — do **not** add it to `application-prd.yaml` unless it's a prd-only value.
- **Logging level in prd**: base yaml defines `logging.level.dev.langchain4j: DEBUG` and similar. Consider overriding `logging.level.root: INFO` in `application-prd.yaml` if you find prd log volume too high. (Tracked in `docker/OPS-DEPLOY-IMPROVEMENTS.md`.)
- **`spring.profiles.group`** is currently **unused**. If you ever need to activate "prd-credentials + prd-metrics" together, define a group in `application.yaml` — but only couple profiles that should always travel together.
- **Flyway `baseline-version`**: env var `FLYWAY_BASELINE_VERSION` overrides. First-time deploy: leave `-1` to run every migration. Legacy DB: set to the version already applied.

## Rollback

See the rollback section of `docs/superpowers/plans/2026-04-17-p2-11-config-consolidation.md` (Task 9). One `git revert` of the consolidation commits restores the pre-refactor yaml shape — resolved properties are unchanged, so no downtime.
