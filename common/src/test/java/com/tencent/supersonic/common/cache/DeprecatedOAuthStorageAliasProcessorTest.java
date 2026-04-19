package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DeprecatedOAuthStorageAliasProcessorTest {

    @Test
    void legacyKeyMappedToNewKey() {
        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("s2.oauth.storage.type", "redis");
        try {
            new DeprecatedOAuthStorageAliasProcessor().postProcessEnvironment(env,
                    new SpringApplication());
            assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isEqualTo("redis");
        } finally {
            env.getSystemProperties().remove("s2.oauth.storage.type");
        }
    }

    @Test
    void newKeyTakesPrecedenceWhenBothPresent() {
        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("s2.oauth.storage.type", "caffeine");
        env.getSystemProperties().put("s2.cache.namespaces.oauth-code.type", "redis");
        try {
            new DeprecatedOAuthStorageAliasProcessor().postProcessEnvironment(env,
                    new SpringApplication());
            assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isEqualTo("redis");
        } finally {
            env.getSystemProperties().remove("s2.oauth.storage.type");
            env.getSystemProperties().remove("s2.cache.namespaces.oauth-code.type");
        }
    }

    @Test
    void absentLegacyKeyIsNoOp() {
        StandardEnvironment env = new StandardEnvironment();
        new DeprecatedOAuthStorageAliasProcessor().postProcessEnvironment(env,
                new SpringApplication());
        assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isNull();
    }
}
