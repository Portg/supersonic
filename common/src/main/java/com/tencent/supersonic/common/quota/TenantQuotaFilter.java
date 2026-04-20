package com.tencent.supersonic.common.quota;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class TenantQuotaFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (TooManyRequestsException e) {
            write429(response, e.getTenantId(), e.getRetryAfterSeconds());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TooManyRequestsException tmre) {
                write429(response, tmre.getTenantId(), tmre.getRetryAfterSeconds());
                return;
            }
            throw e;
        }
    }

    private void write429(ServletResponse response, Long tenantId, int retryAfterSeconds)
            throws IOException {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setStatus(429);
        http.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        http.setContentType("application/json;charset=UTF-8");
        http.getWriter()
                .write("{\"code\":429,\"status\":\"TOO_MANY_REQUESTS\",\"tenantId\":" + tenantId
                        + ",\"retryAfterSeconds\":" + retryAfterSeconds
                        + ",\"msg\":\"Tenant concurrency quota exceeded\"}");
    }
}
