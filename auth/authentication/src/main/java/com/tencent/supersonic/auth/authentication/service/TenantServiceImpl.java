package com.tencent.supersonic.auth.authentication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantDOMapper;
import com.tencent.supersonic.common.util.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of TenantService.
 */
@Service
@Slf4j
public class TenantServiceImpl implements TenantService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_DELETED = "DELETED";

    private final TenantDOMapper tenantDOMapper;

    public TenantServiceImpl(TenantDOMapper tenantDOMapper) {
        this.tenantDOMapper = tenantDOMapper;
    }

    @Override
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        TenantDO tenantDO = convertToTenantDO(tenant);
        tenantDO.setStatus(STATUS_ACTIVE);
        tenantDO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantDOMapper.insert(tenantDO);
        log.info("Created tenant: {} with ID: {}", tenant.getName(), tenantDO.getId());

        return convertToTenant(tenantDO);
    }

    @Override
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        TenantDO tenantDO = convertToTenantDO(tenant);
        tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantDOMapper.updateById(tenantDO);
        log.info("Updated tenant: {}", tenant.getId());

        return convertToTenant(tenantDO);
    }

    @Override
    public Optional<Tenant> getTenantById(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        return Optional.ofNullable(tenantDO).map(this::convertToTenant);
    }

    @Override
    public Optional<Tenant> getTenantByCode(String code) {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDO::getCode, code);
        TenantDO tenantDO = tenantDOMapper.selectOne(wrapper);
        return Optional.ofNullable(tenantDO).map(this::convertToTenant);
    }

    @Override
    public List<Tenant> getAllTenants() {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(TenantDO::getStatus, STATUS_DELETED);
        return tenantDOMapper.selectList(wrapper).stream().map(this::convertToTenant)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tenant> getActiveTenants() {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDO::getStatus, STATUS_ACTIVE);
        return tenantDOMapper.selectList(wrapper).stream().map(this::convertToTenant)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_DELETED);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Deleted tenant: {}", id);
        }
    }

    @Override
    @Transactional
    public void suspendTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_SUSPENDED);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Suspended tenant: {}", id);
        }
    }

    @Override
    @Transactional
    public void activateTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_ACTIVE);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Activated tenant: {}", id);
        }
    }

    @Override
    public boolean isUserLimitReached(Long tenantId) {
        // TODO: Implement by counting users with tenant_id and comparing with max_users
        return false;
    }

    @Override
    public boolean isDatasetLimitReached(Long tenantId) {
        // TODO: Implement by counting datasets with tenant_id and comparing with max_datasets
        return false;
    }

    @Override
    public boolean isModelLimitReached(Long tenantId) {
        // TODO: Implement by counting models with tenant_id and comparing with max_models
        return false;
    }

    @Override
    public boolean isAgentLimitReached(Long tenantId) {
        // TODO: Implement by counting agents with tenant_id and comparing with max_agents
        return false;
    }

    @Override
    public boolean isApiCallLimitReached(Long tenantId) {
        // TODO: Implement by checking today's API call count from usage table
        return false;
    }

    @Override
    public boolean isTenantCodeAvailable(String code) {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDO::getCode, code);
        return tenantDOMapper.selectCount(wrapper) == 0;
    }

    private TenantDO convertToTenantDO(Tenant tenant) {
        TenantDO tenantDO = new TenantDO();
        BeanMapper.mapper(tenant, tenantDO);
        return tenantDO;
    }

    private Tenant convertToTenant(TenantDO tenantDO) {
        Tenant tenant = new Tenant();
        BeanMapper.mapper(tenantDO, tenant);
        return tenant;
    }
}
