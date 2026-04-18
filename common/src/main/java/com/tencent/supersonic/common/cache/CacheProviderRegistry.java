package com.tencent.supersonic.common.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CacheProviderRegistry {

    private final Map<String, CacheProvider> providers;

    public CacheProviderRegistry(Collection<CacheProvider> providers) {
        Map<String, CacheProvider> map = new HashMap<>();
        for (CacheProvider p : providers) {
            if (map.put(p.getName(), p) != null) {
                throw new IllegalStateException("Duplicate cache namespace: " + p.getName()
                        + ". Check s2.cache.namespaces configuration.");
            }
        }
        this.providers = Map.copyOf(map);
    }

    public CacheProvider require(String namespace) {
        CacheProvider p = providers.get(namespace);
        if (p == null) {
            throw new IllegalStateException("No CacheProvider registered for namespace '"
                    + namespace + "'. declared in s2.cache.namespaces? Available: "
                    + providers.keySet());
        }
        return p;
    }

    public Optional<CacheProvider> find(String namespace) {
        return Optional.ofNullable(providers.get(namespace));
    }

    public Map<String, CacheProvider> asMap() {
        return providers;
    }
}
