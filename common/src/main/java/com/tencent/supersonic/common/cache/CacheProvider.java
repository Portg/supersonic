package com.tencent.supersonic.common.cache;

import java.util.Optional;

public interface CacheProvider {
    String getName();

    CacheNamespace getNamespace();

    Optional<String> get(String key);

    void put(String key, String value);

    boolean putIfAbsent(String key, String value);

    void evict(String key);

    long increment(String key);
}
