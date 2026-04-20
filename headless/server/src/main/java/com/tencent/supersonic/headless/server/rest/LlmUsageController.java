package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.llm.CostEstimator;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semantic/admin/llm-usage")
@RequiredArgsConstructor
public class LlmUsageController {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String PLATFORM_QUOTA = "PLATFORM_QUOTA";
    private static final String TENANT_USAGE_VIEW = "TENANT_USAGE_VIEW";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final LlmUsageService llmUsageService;
    private final CostEstimator costEstimator;
    private final UserService userService;

    @GetMapping
    public IPage<LlmUsageDO> query(@RequestParam Long tenantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String callType,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        Long authorizedTenantId =
                authorizeTenant(tenantId, userService.getCurrentUser(request, response));
        return llmUsageService.query(authorizedTenantId, from, to, model, callType, page, size);
    }

    @GetMapping("/daily")
    public List<Map<String, Object>> daily(@RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        Long authorizedTenantId =
                authorizeTenant(tenantId, userService.getCurrentUser(request, response));
        return llmUsageService.dailyAggregates(authorizedTenantId, from, to);
    }

    @GetMapping("/total-tokens")
    public long totalTokens(@RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        Long authorizedTenantId =
                authorizeTenant(tenantId, userService.getCurrentUser(request, response));
        return llmUsageService.sumTokens(authorizedTenantId, from, to);
    }

    @PostMapping("/pricing-cache:refresh")
    public Map<String, Object> refreshPricingCache(@RequestParam(required = false) String provider,
            @RequestParam(required = false) String model, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        authorizePlatform(userService.getCurrentUser(request, response));
        if (provider != null && !provider.isBlank() && model != null && !model.isBlank()) {
            costEstimator.refresh(provider, model);
            return Map.of("refreshed", "one", "provider", provider, "model", model);
        }
        costEstimator.refreshAll();
        return Map.of("refreshed", "all");
    }

    private Long authorizeTenant(Long requestedTenantId, User user) throws IllegalAccessException {
        if (user == null || requestedTenantId == null) {
            throw new IllegalAccessException("只有管理员才能执行此操作");
        }
        if (hasAnyPermission(user, PLATFORM_ADMIN, PLATFORM_QUOTA)) {
            return requestedTenantId;
        }
        if (requestedTenantId.equals(user.getTenantId()) && (user.isSuperAdmin()
                || hasAnyPermission(user, TENANT_ADMIN, TENANT_USAGE_VIEW))) {
            return requestedTenantId;
        }
        throw new IllegalAccessException("无权查看该租户的 LLM 用量");
    }

    private void authorizePlatform(User user) throws IllegalAccessException {
        if (user == null || !hasAnyPermission(user, PLATFORM_ADMIN, PLATFORM_QUOTA)) {
            throw new IllegalAccessException("只有平台管理员才能执行此操作");
        }
    }

    private boolean hasAnyPermission(User user, String... permissions) {
        if (user.getPermissions() == null || user.getPermissions().isEmpty()) {
            return false;
        }
        for (String permission : permissions) {
            if (user.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
