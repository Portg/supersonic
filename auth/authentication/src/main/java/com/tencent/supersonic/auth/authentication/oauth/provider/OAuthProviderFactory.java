package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.api.authentication.config.OAuthProviderConfig;
import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating and managing OAuth providers.
 */
@Slf4j
@Component
public class OAuthProviderFactory {

    private final OAuthConfig oauthConfig;
    private final List<OAuthProvider> providerImplementations;
    private final Map<String, OAuthProvider> initializedProviders = new HashMap<>();

    public OAuthProviderFactory(OAuthConfig oauthConfig,
            List<OAuthProvider> providerImplementations) {
        this.oauthConfig = oauthConfig;
        this.providerImplementations = providerImplementations;
    }

    @PostConstruct
    public void initialize() {
        if (!oauthConfig.isEnabled()) {
            log.info("OAuth is disabled, skipping provider initialization");
            return;
        }

        Map<String, OAuthProviderConfig> providers = oauthConfig.getProviders();
        if (providers == null || providers.isEmpty()) {
            log.warn("OAuth is enabled but no providers are configured");
            return;
        }

        for (Map.Entry<String, OAuthProviderConfig> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            OAuthProviderConfig config = entry.getValue();

            if (!config.isEnabled()) {
                log.info("OAuth provider '{}' is disabled, skipping", providerName);
                continue;
            }

            try {
                OAuthProvider provider = createProvider(config.getType());
                provider.initialize(providerName, config);
                initializedProviders.put(providerName, provider);
                log.info("Initialized OAuth provider: {} (type: {})", providerName,
                        config.getType());
            } catch (Exception e) {
                log.error("Failed to initialize OAuth provider '{}': {}", providerName,
                        e.getMessage());
            }
        }

        log.info("OAuth provider initialization complete. Active providers: {}",
                initializedProviders.keySet());
    }

    /**
     * Get a provider by name.
     */
    public OAuthProvider getProvider(String name) throws OAuthException {
        OAuthProvider provider = initializedProviders.get(name);
        if (provider == null) {
            throw OAuthException.providerNotConfigured(name);
        }
        return provider;
    }

    /**
     * Check if a provider is available.
     */
    public boolean hasProvider(String name) {
        return initializedProviders.containsKey(name);
    }

    /**
     * Get all available provider names.
     */
    public java.util.Set<String> getAvailableProviders() {
        return initializedProviders.keySet();
    }

    /**
     * Create a provider instance based on type.
     */
    private OAuthProvider createProvider(OAuthProviderType type) {
        for (OAuthProvider provider : providerImplementations) {
            if (provider.getType() == type) {
                // Create a new instance for each provider name
                try {
                    return provider.getClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    log.warn("Could not create new instance of {}, using shared instance",
                            provider.getClass().getSimpleName());
                    return provider;
                }
            }
        }
        throw new IllegalArgumentException("No provider implementation found for type: " + type);
    }
}
