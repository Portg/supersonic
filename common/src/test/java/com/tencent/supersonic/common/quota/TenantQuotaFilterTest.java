package com.tencent.supersonic.common.quota;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantQuotaFilterTest {

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
        assertTrue(resp.getContentAsString().contains("TOO_MANY_REQUESTS"));
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
