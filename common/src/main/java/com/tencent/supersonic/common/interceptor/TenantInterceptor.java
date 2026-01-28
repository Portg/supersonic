package com.tencent.supersonic.common.interceptor;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.CurrentUserProvider;
import com.tencent.supersonic.common.util.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * HTTP interceptor that extracts tenant information from request headers, JWT tokens, or subdomains
 * and sets it in the TenantContext for the current thread.
 */
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    /**
     * Get TenantConfig bean lazily at runtime.
     */
    private TenantConfig getTenantConfig() {
        try {
            return ContextUtils.getBean(TenantConfig.class);
        } catch (Exception e) {
            log.error("TenantConfig not available: {}", e.getMessage());
            return null;
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        try {
            CurrentUserProvider provider = ContextUtils.getBean(CurrentUserProvider.class);
            if (provider != null) {
                return provider.getCurrentUser(request, null);
            }
        } catch (Exception e) {
            log.error("Failed to get current user: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        TenantConfig config = getTenantConfig();

        // Skip tenant validation for excluded paths
        if (config != null && config.isExcludedPath(requestUri)) {
            return true;
        }
        Long tenantId = resolveTenantId(request, config);

        if (tenantId != null && tenantId > 0) {
            TenantContext.setTenantId(tenantId);
        } else {
            // Tenant ID not resolved, try to use default
            Long defaultTenantId = config != null ? config.getDefaultTenantId() : 1L;
            TenantContext.setTenantId(defaultTenantId);
            log.warn("Using default tenant: tenantId={} for path={}", defaultTenantId, requestUri);
        }

        return true;
    }

    /**
     * Resolves tenant ID from the request. Supports header-based, token-based, and subdomain
     * resolution.
     */
    private Long resolveTenantId(HttpServletRequest request, TenantConfig config) {
        Long tenantId = null;
        // Try to resolve from header first (highest priority)
        if (config != null && config.isHeaderEnabled()) {
            String headerName = config.getTenantIdHeader();
            String headerValue = request.getHeader(headerName);
            if (StringUtils.isNotBlank(headerValue)) {
                try {
                    tenantId = Long.parseLong(headerValue.trim());
                    return tenantId;
                } catch (NumberFormatException e) {
                    log.warn("[TenantResolve] Invalid tenant ID in header: {}", headerValue);
                }
            }
        }

        // Try to resolve from authenticated user's token
        try {
            User user = getCurrentUser(request);
            if (user != null && user.getTenantId() != null && user.getTenantId() > 0) {
                tenantId = user.getTenantId();
                return tenantId;
            } else if (user != null) {
                log.warn("[TenantResolve] User {} has no valid tenantId (tenantId={})",
                        user.getName(), user.getTenantId());
            } else {
                log.info("[TenantResolve] getCurrentUser returned null");
            }
        } catch (Exception e) {
            log.error("[TenantResolve] Failed to resolve from user token: {}", e.getMessage());
        }

        // Try to resolve from subdomain (if enabled)
        if (config != null && config.isSubdomainEnabled()) {
            tenantId = resolveTenantFromSubdomain(request);
        }

        return tenantId;
    }

    /**
     * Resolves tenant ID from the subdomain. Expected format: {tenant-code}.example.com
     */
    private Long resolveTenantFromSubdomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (StringUtils.isNotBlank(serverName)) {
            // Extract first part of hostname as tenant code
            String[] parts = serverName.split("\\.");
            if (parts.length > 2) {
                String tenantCode = parts[0];
                // TODO: Look up tenant ID from tenant code in database
                log.debug("Extracted tenant code from subdomain: {}", tenantCode);
            }
        }
        return null;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        // No action needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        // Clear tenant context to prevent memory leaks
        TenantContext.clear();
        if (log.isDebugEnabled()) {
            log.debug("Cleared tenant context for request: {}", request.getRequestURI());
        }
    }
}
