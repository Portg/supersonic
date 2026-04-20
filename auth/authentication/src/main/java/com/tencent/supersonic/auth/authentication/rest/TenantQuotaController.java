package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.quota.request.TenantQuotaReq;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.TenantQuotaRepository;
import com.tencent.supersonic.common.quota.TenantQuotaService;
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

    @GetMapping
    public List<TenantQuotaDO> list() {
        return repository.listAll();
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantQuotaDO> get(@PathVariable Long tenantId) {
        return repository.findByTenantId(tenantId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{tenantId}")
    public TenantQuotaDO upsert(@PathVariable Long tenantId, @RequestBody TenantQuotaReq req) {
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
    public ResponseEntity<Void> delete(@PathVariable Long tenantId) {
        repository.findByTenantId(tenantId).ifPresent(q -> repository.removeById(q.getId()));
        TenantQuotaService svc = quotaServiceProvider.getIfAvailable();
        if (svc != null) {
            svc.refresh(tenantId);
        }
        return ResponseEntity.ok().build();
    }
}
