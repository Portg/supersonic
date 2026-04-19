package com.tencent.supersonic.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DeprecatedOAuthStorageAliasProcessor implements EnvironmentPostProcessor {

    private static final String LEGACY_KEY = "s2.oauth.storage.type";
    private static final String NEW_KEY = "s2.cache.namespaces.oauth-code.type";
    private static final String ALIAS_SOURCE = "s2-cache-deprecated-aliases";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String legacy = env.getProperty(LEGACY_KEY);
        if (legacy == null)
            return;

        if (env.getProperty(NEW_KEY) != null) {
            log.warn(
                    "Both '{}' (deprecated) and '{}' are set — the new key wins. Remove '{}' from your configuration.",
                    LEGACY_KEY, NEW_KEY, LEGACY_KEY);
            return;
        }

        log.warn("Configuration key '{}' is deprecated. Migrate to '{}' (value: {}).", LEGACY_KEY,
                NEW_KEY, legacy);

        Map<String, Object> aliases = new HashMap<>();
        aliases.put(NEW_KEY, legacy);
        env.getPropertySources().addFirst(new MapPropertySource(ALIAS_SOURCE, aliases));
    }
}
