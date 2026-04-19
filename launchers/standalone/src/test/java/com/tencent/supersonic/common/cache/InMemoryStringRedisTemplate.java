package com.tencent.supersonic.common.cache;

import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryStringRedisTemplate extends StringRedisTemplate {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private Instant now = Instant.now();

    public InMemoryStringRedisTemplate() {}

    @Override
    public void afterPropertiesSet() {
        // No-op: skip connection factory validation — this template is backed by an in-memory
        // store.
    }

    public void advanceTime(Duration amount) {
        now = now.plus(amount);
        store.entrySet().removeIf(
                e -> e.getValue().expiresAt != null && !e.getValue().expiresAt.isAfter(now));
    }

    private void prune(String key) {
        Entry e = store.get(key);
        if (e != null && e.expiresAt != null && !e.expiresAt.isAfter(now)) {
            store.remove(key);
        }
    }

    @Override
    public ValueOperations<String, String> opsForValue() {
        return new ValueOperations<>() {

            // ── write ────────────────────────────────────────────────────────

            @Override
            public void set(String key, String value) {
                store.put(key, new Entry(value, null));
            }

            @Override
            public void set(String key, String value, long timeout, TimeUnit unit) {
                store.put(key, new Entry(value, now.plusMillis(unit.toMillis(timeout))));
            }

            @Override
            public void set(String key, String value, long offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfAbsent(String key, String value) {
                prune(key);
                return store.putIfAbsent(key, new Entry(value, null)) == null;
            }

            @Override
            public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
                prune(key);
                return store.putIfAbsent(key,
                        new Entry(value, now.plusMillis(unit.toMillis(timeout)))) == null;
            }

            @Override
            public Boolean setIfPresent(String key, String value) {
                prune(key);
                if (!store.containsKey(key))
                    return false;
                store.put(key, new Entry(value, store.get(key).expiresAt));
                return true;
            }

            @Override
            public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
                prune(key);
                if (!store.containsKey(key))
                    return false;
                store.put(key, new Entry(value, now.plusMillis(unit.toMillis(timeout))));
                return true;
            }

            @Override
            public void multiSet(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            // ── read ─────────────────────────────────────────────────────────

            @Override
            public String get(Object key) {
                prune((String) key);
                Entry e = store.get(key);
                return e == null ? null : e.value;
            }

            @Override
            public String getAndDelete(String key) {
                prune(key);
                Entry e = store.remove(key);
                return e == null ? null : e.value;
            }

            @Override
            public String getAndExpire(String key, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndExpire(String key, Duration timeout) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndPersist(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndSet(String key, String newValue) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<String> multiGet(Collection<String> keys) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String get(String key, long start, long end) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long size(String key) {
                throw new UnsupportedOperationException();
            }

            // ── increment / decrement ─────────────────────────────────────────

            @Override
            public Long increment(String key) {
                prune(key);
                Entry e = store.computeIfAbsent(key, k -> new Entry("0", null));
                long v = Long.parseLong(e.value) + 1L;
                store.put(key, new Entry(Long.toString(v), e.expiresAt));
                return v;
            }

            @Override
            public Long increment(String key, long delta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Double increment(String key, double delta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long decrement(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long decrement(String key, long delta) {
                throw new UnsupportedOperationException();
            }

            // ── misc ──────────────────────────────────────────────────────────

            @Override
            public Integer append(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setBit(String key, long offset, boolean value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean getBit(String key, long offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Long> bitField(String key, BitFieldSubCommands subCommands) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RedisOperations<String, String> getOperations() {
                return InMemoryStringRedisTemplate.this;
            }
        };
    }

    // ── key-level operations ──────────────────────────────────────────────────

    @Override
    public Boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public Long delete(Collection<String> keys) {
        long removed = 0;
        for (String k : keys) {
            if (store.remove(k) != null)
                removed++;
        }
        return removed;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        Entry e = store.get(key);
        if (e == null)
            return false;
        store.put(key, new Entry(e.value, now.plusMillis(unit.toMillis(timeout))));
        return true;
    }

    @Override
    public Boolean hasKey(String key) {
        prune(key);
        return store.containsKey(key);
    }

    @Override
    public RedisConnectionFactory getConnectionFactory() {
        return null;
    }

    @Override
    public RedisConnectionFactory getRequiredConnectionFactory() {
        throw new UnsupportedOperationException(
                "InMemoryStringRedisTemplate has no connection factory");
    }

    // ── test helpers ──────────────────────────────────────────────────────────

    public Map<String, String> snapshot() {
        Map<String, String> out = new HashMap<>();
        store.forEach((k, e) -> out.put(k, e.value));
        return Collections.unmodifiableMap(out);
    }

    private record Entry(String value, Instant expiresAt) {}
}
