package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantQuotaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for tenant quota persistence operations.
 */
@Repository
public class TenantQuotaRepository extends ServiceImpl<TenantQuotaMapper, TenantQuotaDO> {

    public Optional<TenantQuotaDO> findByTenantId(Long tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOne(
                new LambdaQueryWrapper<TenantQuotaDO>().eq(TenantQuotaDO::getTenantId, tenantId)));
    }

    public List<TenantQuotaDO> listAll() {
        return list();
    }

    public TenantQuotaDO upsert(TenantQuotaDO desired) {
        Optional<TenantQuotaDO> existing = findByTenantId(desired.getTenantId());
        if (existing.isPresent()) {
            desired.setId(existing.get().getId());
            updateById(desired);
        } else {
            save(desired);
        }
        return desired;
    }
}
