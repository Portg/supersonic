package com.tencent.supersonic.common.cache;

import lombok.Builder;
import lombok.Value;
import java.time.Duration;

@Value
@Builder
public class CacheNamespace {
    String name;
    CacheType typeOverride;
    Duration ttl;
    long maxSize;
    boolean tenantScoped;
}
