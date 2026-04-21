package com.tencent.supersonic.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Enforces the "application.yaml = shared; profile yaml = diff-only" rule.
 *
 * For each profile, declares the exact top-level key prefixes the profile yaml is ALLOWED to
 * define. If a profile yaml starts defining new keys that should have been lifted to base, this
 * test fails with a clear message.
 *
 * Rationale: see docs/runbook/spring-profiles.md and feedback memory "Spring Profile 配置分层规范".
 */
class ProfileYamlDiffTest {

    /**
     * Per-profile allow-lists. Every dotted key in the profile yaml must start with one of these
     * prefixes.
     */
    private static final Map<String, List<String>> ALLOWED_PREFIXES = Map.of("application-h2.yaml",
            List.of("spring.datasource.", "spring.sql.init.", "spring.h2.console.",
                    "spring.flyway.enabled",
                    // H2-specific Quartz dialect: driverDelegateClass + isClustered=false override.
                    "spring.quartz.properties."),
            "application-mysql.yaml",
            List.of("spring.datasource.", "spring.sql.init.", "spring.flyway.enabled",
                    "spring.flyway.locations", "spring.flyway.validate-on-migrate",
                    // MySQL-specific Quartz: driverDelegateClass + selectWithLockSQL.
                    "spring.quartz.properties."),
            "application-postgres.yaml",
            List.of("spring.datasource.", "spring.sql.init.", "spring.flyway.enabled",
                    "spring.flyway.locations", "spring.flyway.validate-on-migrate",
                    "s2.embedding.store.",
                    // Postgres-specific Quartz: PostgreSQLDelegate + selectWithLockSQL.
                    "spring.quartz.properties."),
            "application-dev.yaml",
            List.of("s2.encryption.", "s2.feishu.app-id", "s2.feishu.app-secret",
                    "s2.feishu.verification-token", "s2.feishu.encrypt-key", "spring.mail."),
            "application-prd.yaml",
            List.of("s2.encryption.", "s2.feishu.app-id", "s2.feishu.app-secret",
                    "s2.feishu.verification-token", "s2.feishu.encrypt-key"));

    @Test
    void profileYamlsOnlyDefineAllowedKeys() {
        for (Map.Entry<String, List<String>> entry : ALLOWED_PREFIXES.entrySet()) {
            String fileName = entry.getKey();
            List<String> allowedPrefixes = entry.getValue();
            Set<String> actualKeys = loadFlatKeys(fileName);

            Set<String> violations = actualKeys.stream()
                    .filter(k -> allowedPrefixes.stream().noneMatch(k::startsWith))
                    .collect(Collectors.toCollection(TreeSet::new));

            if (!violations.isEmpty()) {
                fail(String.format(
                        "%s defines keys not in the allow-list — lift them to application.yaml"
                                + " or extend ALLOWED_PREFIXES if genuinely profile-specific:%n  %s%n"
                                + "Allowed prefixes: %s",
                        fileName, String.join("\n  ", violations), allowedPrefixes));
            }
        }
    }

    @Test
    void baseYamlDoesNotDefineProfileOnlyKeys() {
        Set<String> baseKeys = loadFlatKeys("application.yaml");

        List<String> bannedInBase = List.of("spring.datasource.", "spring.sql.init.",
                "spring.h2.console.", "spring.flyway.locations",
                "spring.flyway.validate-on-migrate", "s2.embedding.store.", "s2.encryption.",
                "s2.feishu.app-id", "s2.feishu.app-secret", "s2.feishu.verification-token",
                "s2.feishu.encrypt-key");

        Set<String> leaks = new LinkedHashSet<>();
        for (String key : baseKeys) {
            for (String banned : bannedInBase) {
                if (key.startsWith(banned)) {
                    leaks.add(key);
                }
            }
        }
        assertTrue(leaks.isEmpty(), "application.yaml must not define profile-only keys: " + leaks);
    }

    @Test
    void flywayBaselineLiftedToBase() {
        Set<String> baseKeys = loadFlatKeys("application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.baseline-on-migrate"),
                "spring.flyway.baseline-on-migrate should be defined in application.yaml (shared by mysql+postgres)");
        assertTrue(baseKeys.contains("spring.flyway.baseline-version"),
                "spring.flyway.baseline-version should be defined in application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.table"),
                "spring.flyway.table should be defined in application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.out-of-order"),
                "spring.flyway.out-of-order should be defined in application.yaml");
    }

    private Set<String> loadFlatKeys(String classpathName) {
        Resource res = new ClassPathResource(classpathName);
        if (!res.exists()) {
            throw new IllegalStateException("Missing yaml on classpath: " + classpathName);
        }
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(res);
        Properties props = yaml.getObject();
        if (props == null) {
            return Set.of();
        }
        return props.stringPropertyNames();
    }
}
