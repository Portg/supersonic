package com.tencent.supersonic.billing.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.billing.api.pojo.SubscriptionPlan;
import com.tencent.supersonic.billing.api.pojo.TenantSubscription;
import com.tencent.supersonic.billing.api.service.SubscriptionService;
import com.tencent.supersonic.billing.server.persistence.dataobject.SubscriptionPlanDO;
import com.tencent.supersonic.billing.server.persistence.dataobject.TenantSubscriptionDO;
import com.tencent.supersonic.billing.server.persistence.mapper.SubscriptionPlanDOMapper;
import com.tencent.supersonic.billing.server.persistence.mapper.TenantSubscriptionDOMapper;
import com.tencent.supersonic.common.util.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of SubscriptionService.
 */
@Service
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_DELETED = "DELETED";

    private final SubscriptionPlanDOMapper subscriptionPlanDOMapper;
    private final TenantSubscriptionDOMapper tenantSubscriptionDOMapper;

    public SubscriptionServiceImpl(SubscriptionPlanDOMapper subscriptionPlanDOMapper,
            TenantSubscriptionDOMapper tenantSubscriptionDOMapper) {
        this.subscriptionPlanDOMapper = subscriptionPlanDOMapper;
        this.tenantSubscriptionDOMapper = tenantSubscriptionDOMapper;
    }

    @Override
    public List<SubscriptionPlan> getAllPlans() {
        LambdaQueryWrapper<SubscriptionPlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SubscriptionPlanDO::getStatus, STATUS_DELETED);
        return subscriptionPlanDOMapper.selectList(wrapper).stream().map(this::convertToPlan)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionPlan> getActivePlans() {
        LambdaQueryWrapper<SubscriptionPlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlanDO::getStatus, STATUS_ACTIVE);
        return subscriptionPlanDOMapper.selectList(wrapper).stream().map(this::convertToPlan)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SubscriptionPlan> getPlanById(Long planId) {
        SubscriptionPlanDO planDO = subscriptionPlanDOMapper.selectById(planId);
        return Optional.ofNullable(planDO).map(this::convertToPlan);
    }

    @Override
    public Optional<SubscriptionPlan> getPlanByCode(String code) {
        LambdaQueryWrapper<SubscriptionPlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlanDO::getCode, code);
        SubscriptionPlanDO planDO = subscriptionPlanDOMapper.selectOne(wrapper);
        return Optional.ofNullable(planDO).map(this::convertToPlan);
    }

    @Override
    public SubscriptionPlan getDefaultPlan() {
        LambdaQueryWrapper<SubscriptionPlanDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlanDO::getIsDefault, true);
        SubscriptionPlanDO planDO = subscriptionPlanDOMapper.selectOne(wrapper);
        if (planDO == null) {
            // Fall back to FREE plan
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SubscriptionPlanDO::getCode, "FREE");
            planDO = subscriptionPlanDOMapper.selectOne(wrapper);
        }
        return planDO != null ? convertToPlan(planDO) : null;
    }

    @Override
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        SubscriptionPlanDO planDO = convertToPlanDO(plan);
        planDO.setStatus(STATUS_ACTIVE);
        planDO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        planDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        subscriptionPlanDOMapper.insert(planDO);
        log.info("Created subscription plan: {} with code: {}", plan.getName(), plan.getCode());

        return convertToPlan(planDO);
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(SubscriptionPlan plan) {
        SubscriptionPlanDO planDO = convertToPlanDO(plan);
        planDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        subscriptionPlanDOMapper.updateById(planDO);
        log.info("Updated subscription plan: {}", plan.getId());

        return convertToPlan(subscriptionPlanDOMapper.selectById(plan.getId()));
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlanDO planDO = subscriptionPlanDOMapper.selectById(planId);
        if (planDO != null) {
            // Soft delete by setting status to DELETED
            planDO.setStatus(STATUS_DELETED);
            planDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            subscriptionPlanDOMapper.updateById(planDO);
            log.info("Deleted subscription plan: {}", planId);
        }
    }

    @Override
    @Transactional
    public TenantSubscription createSubscription(TenantSubscription subscription) {
        TenantSubscriptionDO subscriptionDO = convertToSubscriptionDO(subscription);
        subscriptionDO.setStatus(STATUS_ACTIVE);
        subscriptionDO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        subscriptionDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantSubscriptionDOMapper.insert(subscriptionDO);
        log.info("Created subscription for tenant: {} with plan: {}", subscription.getTenantId(),
                subscription.getPlanId());

        return convertToSubscription(subscriptionDO);
    }

    @Override
    @Transactional
    public TenantSubscription updateSubscription(TenantSubscription subscription) {
        TenantSubscriptionDO subscriptionDO = convertToSubscriptionDO(subscription);
        subscriptionDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantSubscriptionDOMapper.updateById(subscriptionDO);
        log.info("Updated subscription: {}", subscription.getId());

        return convertToSubscription(subscriptionDO);
    }

    @Override
    public Optional<TenantSubscription> getActiveSubscription(Long tenantId) {
        LambdaQueryWrapper<TenantSubscriptionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantSubscriptionDO::getTenantId, tenantId)
                .eq(TenantSubscriptionDO::getStatus, STATUS_ACTIVE)
                .orderByDesc(TenantSubscriptionDO::getCreatedAt).last("LIMIT 1");
        TenantSubscriptionDO subscriptionDO = tenantSubscriptionDOMapper.selectOne(wrapper);

        if (subscriptionDO != null) {
            TenantSubscription subscription = convertToSubscription(subscriptionDO);
            // Enrich with plan name
            getPlanById(subscriptionDO.getPlanId())
                    .ifPresent(plan -> subscription.setPlanName(plan.getName()));
            return Optional.of(subscription);
        }
        return Optional.empty();
    }

    @Override
    public List<TenantSubscription> getSubscriptionsByTenant(Long tenantId) {
        LambdaQueryWrapper<TenantSubscriptionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantSubscriptionDO::getTenantId, tenantId)
                .orderByDesc(TenantSubscriptionDO::getCreatedAt);
        return tenantSubscriptionDOMapper.selectList(wrapper).stream()
                .map(this::convertToSubscription).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        TenantSubscriptionDO subscriptionDO = tenantSubscriptionDOMapper.selectById(subscriptionId);
        if (subscriptionDO != null) {
            subscriptionDO.setStatus(STATUS_CANCELLED);
            subscriptionDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantSubscriptionDOMapper.updateById(subscriptionDO);
            log.info("Cancelled subscription: {}", subscriptionId);
        }
    }

    @Override
    @Transactional
    public TenantSubscription changeSubscription(Long tenantId, Long newPlanId,
            String billingCycle) {
        // Cancel existing subscription
        getActiveSubscription(tenantId).ifPresent(existing -> cancelSubscription(existing.getId()));

        // Create new subscription
        TenantSubscription newSubscription =
                TenantSubscription.builder().tenantId(tenantId).planId(newPlanId)
                        .status(STATUS_ACTIVE).startDate(new Timestamp(System.currentTimeMillis()))
                        .billingCycle(billingCycle != null ? billingCycle : "MONTHLY")
                        .autoRenew(true).build();

        return createSubscription(newSubscription);
    }

    @Override
    @Transactional
    public TenantSubscription assignSubscription(Long tenantId, Long planId, String billingCycle) {
        log.info("Admin assigned subscription for tenant: {} with plan: {}", tenantId, planId);
        return changeSubscription(tenantId, planId, billingCycle);
    }

    private SubscriptionPlan convertToPlan(SubscriptionPlanDO planDO) {
        SubscriptionPlan plan = new SubscriptionPlan();
        BeanMapper.mapper(planDO, plan);
        return plan;
    }

    private SubscriptionPlanDO convertToPlanDO(SubscriptionPlan plan) {
        SubscriptionPlanDO planDO = new SubscriptionPlanDO();
        BeanMapper.mapper(plan, planDO);
        return planDO;
    }

    private TenantSubscription convertToSubscription(TenantSubscriptionDO subscriptionDO) {
        TenantSubscription subscription = new TenantSubscription();
        BeanMapper.mapper(subscriptionDO, subscription);
        return subscription;
    }

    private TenantSubscriptionDO convertToSubscriptionDO(TenantSubscription subscription) {
        TenantSubscriptionDO subscriptionDO = new TenantSubscriptionDO();
        BeanMapper.mapper(subscription, subscriptionDO);
        return subscriptionDO;
    }
}
