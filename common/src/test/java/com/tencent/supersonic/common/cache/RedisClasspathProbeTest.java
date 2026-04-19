package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class RedisClasspathProbeTest {
    @Test
    void stringRedisTemplateIsOnClasspath() {
        assertThatCode(
                () -> Class.forName("org.springframework.data.redis.core.StringRedisTemplate"))
                        .doesNotThrowAnyException();
    }
}
