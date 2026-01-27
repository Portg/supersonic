package com.tencent.supersonic.auth.authentication.oauth.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OAuth exchange code storage. Uses Caffeine in-memory cache as default. For
 * Redis-based storage, use RedisCodeExchangeStorageConfig which is conditionally loaded when Redis
 * is available.
 */
@Slf4j
@Configuration
public class CodeExchangeStorageConfig {

    /**
     * Caffeine-based storage (default fallback).
     */
    @Bean
    @ConditionalOnMissingBean(CodeExchangeStorage.class)
    public CodeExchangeStorage caffeineCodeExchangeStorage() {
        log.info("Using Caffeine-based CodeExchangeStorage for OAuth exchange codes");
        return new CaffeineCodeExchangeStorage();
    }
}
