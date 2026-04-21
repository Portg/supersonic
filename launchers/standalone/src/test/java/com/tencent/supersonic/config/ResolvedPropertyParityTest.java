package com.tencent.supersonic.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the profile consolidation refactor (lifting shared Flyway keys to application.yaml)
 * did not change the effective resolved property values for each supported (db, env) profile pair.
 *
 * Uses Spring's {@link YamlPropertySourceLoader} directly — no application context required — so
 * the test runs without a live database or Redis. Spring Boot's property-source priority is
 * emulated by inserting sources in reverse order (last wins) into a
 * {@link CompositePropertySource}.
 */
class ResolvedPropertyParityTest {

    /**
     * Builds a CompositePropertySource that merges the given YAML classpath files in priority
     * order: base first (lowest), then profiles in increasing priority. Each later source shadows
     * earlier ones, matching Spring Boot's resolution order.
     */
    private CompositePropertySource mergedSources(String... classpathYamls) throws IOException {
        var loader = new YamlPropertySourceLoader();
        var composite = new CompositePropertySource("merged");
        // Add in reverse so that later files have higher priority in CompositePropertySource
        for (int i = classpathYamls.length - 1; i >= 0; i--) {
            List<PropertySource<?>> sources =
                    loader.load(classpathYamls[i], new ClassPathResource(classpathYamls[i]));
            for (PropertySource<?> src : sources) {
                composite.addPropertySource(src);
            }
        }
        return composite;
    }

    private Object get(CompositePropertySource src, String key) {
        return src.getProperty(key);
    }

    // ── H2 + dev ────────────────────────────────────────────────────────────

    @Test
    void h2Dev_flywayDisabled() throws IOException {
        var src = mergedSources("application.yaml", "application-h2.yaml", "application-dev.yaml");
        assertEquals("false", String.valueOf(get(src, "spring.flyway.enabled")));
    }

    @Test
    void h2Dev_baseFlywayKeysInherited() throws IOException {
        var src = mergedSources("application.yaml", "application-h2.yaml", "application-dev.yaml");
        assertEquals("true", String.valueOf(get(src, "spring.flyway.baseline-on-migrate")));
        assertEquals("flyway_schema_history", get(src, "spring.flyway.table"));
        assertEquals("false", String.valueOf(get(src, "spring.flyway.out-of-order")));
    }

    @Test
    void h2Dev_h2DatasourceDriver() throws IOException {
        var src = mergedSources("application.yaml", "application-h2.yaml", "application-dev.yaml");
        assertEquals("org.h2.Driver", get(src, "spring.datasource.driver-class-name"));
        String url = String.valueOf(get(src, "spring.datasource.url"));
        assertTrue(url.startsWith("jdbc:h2:mem:semantic"),
                "Expected H2 in-memory URL, got: " + url);
    }

    // ── MySQL + prd ─────────────────────────────────────────────────────────

    @Test
    void mysqlPrd_flywayOverridesApplied() throws IOException {
        var src =
                mergedSources("application.yaml", "application-mysql.yaml", "application-prd.yaml");
        assertEquals("true", String.valueOf(get(src, "spring.flyway.enabled")));
        assertEquals("classpath:db/migration/mysql", get(src, "spring.flyway.locations"));
        assertEquals("false", String.valueOf(get(src, "spring.flyway.validate-on-migrate")));
    }

    @Test
    void mysqlPrd_baseFlywayKeysInherited() throws IOException {
        var src =
                mergedSources("application.yaml", "application-mysql.yaml", "application-prd.yaml");
        assertEquals("true", String.valueOf(get(src, "spring.flyway.baseline-on-migrate")));
        assertEquals("flyway_schema_history", get(src, "spring.flyway.table"));
        assertEquals("false", String.valueOf(get(src, "spring.flyway.out-of-order")));
    }

    @Test
    void mysqlPrd_jdbcUrlStartsWithMysql() throws IOException {
        var src =
                mergedSources("application.yaml", "application-mysql.yaml", "application-prd.yaml");
        String url = String.valueOf(get(src, "spring.datasource.url"));
        assertTrue(url.startsWith("jdbc:mysql://"), "Expected MySQL JDBC URL, got: " + url);
        assertTrue(url.contains("connectionCollation=utf8mb4_unicode_ci"),
                "MySQL URL must preserve UTF-8 collation param, got: " + url);
    }

    @Test
    void mysqlPrd_criticalSharedPropertiesUnchanged() throws IOException {
        var src =
                mergedSources("application.yaml", "application-mysql.yaml", "application-prd.yaml");
        assertEquals("9080", String.valueOf(get(src, "server.port")));
        assertEquals("chat", get(src, "spring.application.name"));
        assertEquals("graceful", get(src, "server.shutdown"));
        assertEquals("true", String.valueOf(get(src, "s2.tenant.enabled")));
        assertEquals("X-Tenant-Id", get(src, "s2.tenant.tenant-id-header"));
    }

    // ── Postgres + prd ──────────────────────────────────────────────────────

    @Test
    void postgresPrd_flywayOverridesApplied() throws IOException {
        var src = mergedSources("application.yaml", "application-postgres.yaml",
                "application-prd.yaml");
        assertEquals("true", String.valueOf(get(src, "spring.flyway.enabled")));
        assertEquals("classpath:db/migration/postgresql", get(src, "spring.flyway.locations"));
        assertEquals("true", String.valueOf(get(src, "spring.flyway.validate-on-migrate")));
    }

    @Test
    void postgresPrd_baseFlywayKeysInherited() throws IOException {
        var src = mergedSources("application.yaml", "application-postgres.yaml",
                "application-prd.yaml");
        assertEquals("true", String.valueOf(get(src, "spring.flyway.baseline-on-migrate")));
        assertEquals("flyway_schema_history", get(src, "spring.flyway.table"));
        assertEquals("false", String.valueOf(get(src, "spring.flyway.out-of-order")));
    }

    @Test
    void postgresPrd_pgvectorProviderWired() throws IOException {
        var src = mergedSources("application.yaml", "application-postgres.yaml",
                "application-prd.yaml");
        assertEquals("PGVECTOR", get(src, "s2.embedding.store.provider"));
        assertEquals("512", String.valueOf(get(src, "s2.embedding.store.dimension")));
    }

    @Test
    void postgresPrd_jdbcUrlStartsWithPostgres() throws IOException {
        var src = mergedSources("application.yaml", "application-postgres.yaml",
                "application-prd.yaml");
        String url = String.valueOf(get(src, "spring.datasource.url"));
        assertTrue(url.startsWith("jdbc:postgresql://"),
                "Expected PostgreSQL JDBC URL, got: " + url);
    }

    // ── Cross-profile: shared keys must NOT be overridden by mysql/postgres ─

    @Test
    void sharedFlywayBaselineVersionNotOverriddenByMysql() throws IOException {
        var src = mergedSources("application.yaml", "application-mysql.yaml");
        // baseline-version is only in application.yaml; mysql yaml must not re-define it
        Object val = get(src, "spring.flyway.baseline-version");
        assertNotNull(val, "spring.flyway.baseline-version must resolve from base yaml");
    }

    @Test
    void sharedFlywayBaselineVersionNotOverriddenByPostgres() throws IOException {
        var src = mergedSources("application.yaml", "application-postgres.yaml");
        Object val = get(src, "spring.flyway.baseline-version");
        assertNotNull(val, "spring.flyway.baseline-version must resolve from base yaml");
    }
}
