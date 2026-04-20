package com.tencent.supersonic.common.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantQuotaFilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void translatesExceptionTo429WithRetryAfter() throws ServletException, IOException {
        TenantQuotaFilter filter = new TenantQuotaFilter();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/semantic/query");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> {
            throw new TooManyRequestsException(42L, 5);
        };

        filter.doFilter(req, resp, chain);

        assertEquals(429, resp.getStatus());
        assertEquals("5", resp.getHeader("Retry-After"));
        assertEquals("TOO_MANY_REQUESTS",
                OBJECT_MAPPER.readValue(resp.getContentAsString(), Map.class).get("status"));
    }

    @Test
    void propagatesUnrelatedExceptions() {
        TenantQuotaFilter filter = new TenantQuotaFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> {
            throw new RuntimeException("unrelated");
        };

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> filter.doFilter(req, resp, chain));
        assertEquals("unrelated", ex.getMessage());
    }
}
