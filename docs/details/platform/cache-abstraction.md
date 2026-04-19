---
status: active
module: platform/cache
key-files:
  - common/src/main/java/com/tencent/supersonic/common/cache/CacheProvider.java
  - common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfiguration.java
  - common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheProperties.java
---

# Unified Cache Abstraction

SuperSonic provides a single cache SPI (`CacheProvider`) with pluggable Caffeine/Redis backends,
configured under `s2.cache.*`. Each business domain declares a named namespace; the framework
instantiates one `CacheProvider` bean per namespace according to the global default
(`s2.cache.type`) and any per-namespace overrides.

## Configuration

```yaml
s2:
  cache:
    type: redis            # global default: caffeine | redis
    namespaces:
      oauth-code:
        type: caffeine     # per-namespace override
        ttl: 45s
        max-size: 2000
        tenant-scoped: false
      semantic-query:
        ttl: 10m
        max-size: 5000
        tenant-scoped: true
```

## Built-in namespaces

| Namespace | Consumer | Default TTL | Default MaxSize | Tenant-scoped |
|-----------|----------|------------:|----------------:|:-------------:|
| `feishu-event-dedup` | Feishu webhook dedup | 5m | 10 000 | no |
| `feishu-token` | `FeishuTokenManager` | 110m | 16 | no |
| `feishu-general` | `FeishuBindTokenService`, generic KV | 30m | 5 000 | no |
| `feishu-counter` | `FeishuApiRateLimiter` | 60s | 10 000 | no |
| `oauth-code` | `OAuthCodeExchangeService` | 60s | 10 000 | no |
| `semantic-query` | `DefaultQueryCache` | 10m | 5 000 | yes |

## Tenant-scoped keys

When `tenant-scoped: true`, key prefix becomes `tenant:<tenantId>:`. When no tenant context is
set, `tenant:_:` is used — preventing cross-tenant bleed.

OAuth exchange codes and Feishu tokens are NOT tenant-scoped.

## Deprecated properties

| Old property | New property |
|--------------|--------------|
| `s2.oauth.storage.type` | `s2.cache.namespaces.oauth-code.type` |
| `s2.feishu.cache.type` | `s2.cache.namespaces.feishu-*.type` (set each individually) |

`s2.oauth.storage.type` is auto-aliased at startup (`DeprecatedOAuthStorageAliasProcessor`).
`s2.feishu.cache.type` is NOT auto-aliased — migrate manually.

## Adding a new namespace

1. Declare in YAML:
   ```yaml
   s2.cache.namespaces.my-new-cache:
     ttl: 1m
     max-size: 1000
     tenant-scoped: true
   ```
2. Inject in your service:
   ```java
   private final CacheProvider cache;

   public MyService(CacheProviderRegistry registry) {
       this.cache = registry.require("my-new-cache");
   }
   ```
