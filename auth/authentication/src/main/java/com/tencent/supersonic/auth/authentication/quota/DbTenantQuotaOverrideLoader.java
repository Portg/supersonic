package com.tencent.supersonic.auth.authentication.quota;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.TenantQuotaRepository;
import com.tencent.supersonic.common.quota.TenantQuotaOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Loads per-tenant quota overrides from the s2_tenant_quota table. Registered as a Spring bean so
 * that TenantQuotaAutoConfiguration can inject it via ObjectProvider.
 */
@Component
@RequiredArgsConstructor
public class DbTenantQuotaOverrideLoader implements Function<Long, TenantQuotaOverride> {

    private final TenantQuotaRepository repository;

    @Override
    public TenantQuotaOverride apply(Long tenantId) {
        Optional<TenantQuotaDO> row = repository.findByTenantId(tenantId);
        if (row.isEmpty()) {
            return null;
        }
        TenantQuotaDO do_ = row.get();
        TenantQuotaOverride override = new TenantQuotaOverride();
        if (do_.getJdbcConcurrent() != null) {
            override.setJdbcConcurrent(do_.getJdbcConcurrent());
        }
        if (do_.getLlmConcurrent() != null) {
            override.setLlmConcurrent(do_.getLlmConcurrent());
        }
        if (do_.getAcquireTimeoutMs() != null) {
            override.setAcquireTimeoutMs(do_.getAcquireTimeoutMs());
        }
        if (do_.getEnabled() != null) {
            override.setEnabled(do_.getEnabled());
        }
        return override;
    }
}
