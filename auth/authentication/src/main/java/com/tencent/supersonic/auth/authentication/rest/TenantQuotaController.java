package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.quota.request.TenantQuotaReq;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.TenantQuotaRepository;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.quota.TenantQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin REST controller for per-tenant concurrency quota management (P1-5).
 */
@RestController
@RequestMapping("/api/v1/admin/tenant-quotas")
@RequiredArgsConstructor
public class TenantQuotaController {

    private final TenantQuotaRepository repository;
    private final ObjectProvider<TenantQuotaService> quotaServiceProvider;
    private final UserService userService;

    @GetMapping
    public List<TenantQuotaDO> list(HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        return repository.listAll();
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantQuotaDO> get(@PathVariable Long tenantId,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        return repository.findByTenantId(tenantId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{tenantId}")
    public TenantQuotaDO upsert(@PathVariable Long tenantId, @RequestBody TenantQuotaReq req,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        validate(tenantId, req);
        TenantQuotaDO d = new TenantQuotaDO();
        d.setTenantId(tenantId);
        d.setJdbcConcurrent(req.getJdbcConcurrent());
        d.setLlmConcurrent(req.getLlmConcurrent());
        d.setMonthlyQueryCount(req.getMonthlyQueryCount());
        d.setAcquireTimeoutMs(req.getAcquireTimeoutMs());
        d.setEnabled(req.getEnabled());
        d.setUpdatedAt(LocalDateTime.now());
        TenantQuotaDO saved = repository.upsert(d);
        TenantQuotaService svc = quotaServiceProvider.getIfAvailable();
        if (svc != null) {
            svc.refresh(tenantId);
        }
        return saved;
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> delete(@PathVariable Long tenantId, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        repository.findByTenantId(tenantId).ifPresent(q -> repository.removeById(q.getId()));
        TenantQuotaService svc = quotaServiceProvider.getIfAvailable();
        if (svc != null) {
            svc.refresh(tenantId);
        }
        return ResponseEntity.ok().build();
    }

    private void validate(Long tenantId, TenantQuotaReq req) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        // jdbcConcurrent is the primary enforcement knob — required and must be positive.
        requirePositive(req.getJdbcConcurrent(), "jdbcConcurrent");
        // llmConcurrent, acquireTimeoutMs, monthlyQueryCount are optional;
        // null means "use the global default from s2.tenant.quota.default.*".
        if (req.getLlmConcurrent() != null && req.getLlmConcurrent() <= 0) {
            throw new IllegalArgumentException("llmConcurrent must be positive when provided");
        }
        if (req.getAcquireTimeoutMs() != null && req.getAcquireTimeoutMs() <= 0) {
            throw new IllegalArgumentException("acquireTimeoutMs must be positive when provided");
        }
        if (req.getMonthlyQueryCount() != null && req.getMonthlyQueryCount() < 0) {
            throw new IllegalArgumentException("monthlyQueryCount must be zero or positive");
        }
        if (req.getEnabled() == null) {
            throw new IllegalArgumentException("enabled is required");
        }
    }

    private void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private void checkAdminPermission(User user) throws IllegalAccessException {
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            throw new IllegalAccessException("只有管理员才能执行此操作");
        }
    }
}
