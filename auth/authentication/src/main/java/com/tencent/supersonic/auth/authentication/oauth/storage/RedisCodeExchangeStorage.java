package com.tencent.supersonic.auth.authentication.oauth.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of CodeExchangeStorage. Suitable for distributed/clustered deployments
 * where multiple instances need to share state.
 */
@Slf4j
public class RedisCodeExchangeStorage implements CodeExchangeStorage {

    private static final String KEY_PREFIX = "oauth:exchange:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCodeExchangeStorage(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        log.info("Initialized Redis-based CodeExchangeStorage");
    }

    @Override
    public void store(String code, OAuthCodeExchange exchange, long ttlSeconds) {
        try {
            String key = KEY_PREFIX + code;
            String value = objectMapper.writeValueAsString(exchange);
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Stored exchange code in Redis: {}", code);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize exchange code: {}", e.getMessage());
            throw new RuntimeException("Failed to store exchange code", e);
        }
    }

    @Override
    public Optional<OAuthCodeExchange> getAndRemove(String code) {
        String key = KEY_PREFIX + code;
        String value = redisTemplate.opsForValue().getAndDelete(key);

        if (value == null) {
            log.debug("Exchange code not found in Redis: {}", code);
            return Optional.empty();
        }

        try {
            OAuthCodeExchange exchange = objectMapper.readValue(value, OAuthCodeExchange.class);
            if (exchange.isValid()) {
                exchange.setUsed(true);
                log.debug("Retrieved and removed exchange code from Redis: {}", code);
                return Optional.of(exchange);
            }
            log.debug("Exchange code expired or already used: {}", code);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize exchange code: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void remove(String code) {
        String key = KEY_PREFIX + code;
        redisTemplate.delete(key);
        log.debug("Removed exchange code from Redis: {}", code);
    }

    @Override
    public String getStorageType() {
        return "redis";
    }
}
