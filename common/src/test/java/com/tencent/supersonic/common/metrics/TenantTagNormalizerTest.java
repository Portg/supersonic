package com.tencent.supersonic.common.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantTagNormalizerTest {

    @Test
    void returnsTenantWhenInAllowlist() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a", "b"), 50, true);
        assertEquals("a", n.normalize("a"));
    }

    @Test
    void returnsOtherWhenNotInAllowlistAndListFull() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a"), 1, true);
        assertEquals("other", n.normalize("zzz"));
    }

    @Test
    void returnsNoneWhenInputNullOrBlank() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of(), 50, true);
        assertEquals("none", n.normalize(null));
        assertEquals("none", n.normalize("  "));
    }

    @Test
    void returnsDisabledWhenEmitDisabled() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a"), 50, false);
        assertEquals("disabled", n.normalize("a"));
    }

    @Test
    void admitsUpToLimitWhenAllowlistEmpty() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of(), 2, true);
        assertEquals("x", n.normalize("x"));
        assertEquals("y", n.normalize("y"));
        assertEquals("other", n.normalize("z"));
    }
}
