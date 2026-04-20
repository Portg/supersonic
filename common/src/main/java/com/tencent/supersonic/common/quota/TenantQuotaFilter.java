package com.tencent.supersonic.common.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TenantQuotaFilter implements Filter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (TooManyRequestsException e) {
            write429(response, e.getTenantId(), e.getRetryAfterSeconds());
        } catch (RuntimeException e) {
            TooManyRequestsException tmre = findTooManyRequests(e);
            if (tmre != null) {
                write429(response, tmre.getTenantId(), tmre.getRetryAfterSeconds());
                return;
            }
            throw e;
        }
    }

    private TooManyRequestsException findTooManyRequests(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TooManyRequestsException tmre) {
                return tmre;
            }
            current = current.getCause();
        }
        return null;
    }

    private void write429(ServletResponse response, Long tenantId, int retryAfterSeconds)
            throws IOException {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setStatus(429);
        http.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        http.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 429);
        body.put("status", "TOO_MANY_REQUESTS");
        body.put("tenantId", tenantId);
        body.put("retryAfterSeconds", retryAfterSeconds);
        body.put("msg", "Tenant concurrency quota exceeded");
        OBJECT_MAPPER.writeValue(http.getWriter(), body);
    }
}
