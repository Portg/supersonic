package com.tencent.supersonic.auth.api.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth 2.0 / OIDC configuration.
 */
@Data
@ConfigurationProperties(prefix = "s2.authentication.oauth")
public class OAuthConfig {

    /**
     * Whether OAuth authentication is enabled
     */
    private boolean enabled = false;

    /**
     * Default OAuth provider to use
     */
    private String defaultProvider;

    /**
     * Base URL for OAuth callbacks (e.g., http://localhost:9080)
     */
    private String callbackBaseUrl;

    /**
     * OAuth state timeout in milliseconds (default: 5 minutes)
     */
    private long stateTimeout = 300000;

    /**
     * Map of provider name to provider configuration
     */
    private Map<String, OAuthProviderConfig> providers = new HashMap<>();

    /**
     * Get a specific provider configuration by name.
     */
    public OAuthProviderConfig getProvider(String name) {
        return providers.get(name);
    }

    /**
     * Get the default provider configuration.
     */
    public OAuthProviderConfig getDefaultProviderConfig() {
        if (defaultProvider == null || defaultProvider.isEmpty()) {
            return null;
        }
        return providers.get(defaultProvider);
    }

    /**
     * Check if a specific provider is configured and enabled.
     */
    public boolean isProviderEnabled(String name) {
        OAuthProviderConfig config = providers.get(name);
        return config != null && config.isEnabled();
    }

    /**
     * Get the OAuth callback URL for a specific provider.
     */
    public String getCallbackUrl(String providerName) {
        String baseUrl = callbackBaseUrl;
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:9080";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/api/auth/oauth/callback/" + providerName;
    }

    /**
     * Validate the OAuth configuration.
     */
    public void validate() {
        if (!enabled) {
            return;
        }

        if (providers.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one OAuth provider must be configured when OAuth is enabled");
        }

        for (Map.Entry<String, OAuthProviderConfig> entry : providers.entrySet()) {
            String name = entry.getKey();
            OAuthProviderConfig config = entry.getValue();
            try {
                config.validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid configuration for OAuth provider '"
                        + name + "': " + e.getMessage());
            }
        }

        if (defaultProvider != null && !defaultProvider.isEmpty()
                && !providers.containsKey(defaultProvider)) {
            throw new IllegalArgumentException(
                    "Default OAuth provider '" + defaultProvider + "' is not configured");
        }
    }
}
