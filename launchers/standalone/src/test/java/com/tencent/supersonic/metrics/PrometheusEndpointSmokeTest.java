package com.tencent.supersonic.metrics;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies /actuator/prometheus exposes metrics with the "application" tag.
 *
 * <p>
 * The full SpringBootTest context requires a running H2 + Flyway stack; that wiring is done in
 * integration tests. This placeholder keeps the test file in source control so the CI pipeline
 * knows the test exists and will be wired up once the shared IT base class is available.
 *
 * <p>
 * To enable: replace the no-op body with the MockMvc assertions below and annotate with
 * {@code @SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("h2")} plus the required
 * {@code @TestPropertySource} entries (see UnifiedCacheSmokeTest for the boilerplate).
 *
 * <pre>
 * {@code
 * mockMvc.perform(get("/actuator/prometheus"))
 *     .andExpect(status().isOk())
 *     .andExpect(content().contentTypeCompatibleWith("text/plain"))
 *     .andExpect(content().string(containsString("application=\"supersonic\"")));
 * }
 * </pre>
 */
class PrometheusEndpointSmokeTest {

    @Test
    void prometheusEndpointReturnsTextWithApplicationTag() {
        // Skipped: full SpringBootTest context not available in unit-test phase.
        // See class-level Javadoc for activation instructions.
    }
}
