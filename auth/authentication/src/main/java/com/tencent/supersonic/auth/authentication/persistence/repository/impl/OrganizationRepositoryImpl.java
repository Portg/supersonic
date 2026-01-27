package com.tencent.supersonic.auth.authentication.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserOrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.OrganizationDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserOrganizationDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.repository.OrganizationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrganizationRepositoryImpl implements OrganizationRepository {

    private final OrganizationDOMapper organizationDOMapper;
    private final UserOrganizationDOMapper userOrganizationDOMapper;

    public OrganizationRepositoryImpl(OrganizationDOMapper organizationDOMapper,
            UserOrganizationDOMapper userOrganizationDOMapper) {
        this.organizationDOMapper = organizationDOMapper;
        this.userOrganizationDOMapper = userOrganizationDOMapper;
    }

    @Override
    public List<OrganizationDO> getOrganizationList() {
        LambdaQueryWrapper<OrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrganizationDO::getStatus, 1).orderByAsc(OrganizationDO::getSortOrder);
        return organizationDOMapper.selectList(queryWrapper);
    }

    @Override
    public List<OrganizationDO> getOrganizationListByTenantId(Long tenantId) {
        LambdaQueryWrapper<OrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrganizationDO::getTenantId, tenantId).eq(OrganizationDO::getStatus, 1)
                .orderByAsc(OrganizationDO::getSortOrder);
        return organizationDOMapper.selectList(queryWrapper);
    }

    @Override
    public OrganizationDO getOrganization(Long id) {
        return organizationDOMapper.selectById(id);
    }

    @Override
    public List<OrganizationDO> getOrganizationsByParentId(Long parentId) {
        LambdaQueryWrapper<OrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrganizationDO::getParentId, parentId).eq(OrganizationDO::getStatus, 1)
                .orderByAsc(OrganizationDO::getSortOrder);
        return organizationDOMapper.selectList(queryWrapper);
    }

    @Override
    public void addOrganization(OrganizationDO organizationDO) {
        organizationDOMapper.insert(organizationDO);
    }

    @Override
    public void updateOrganization(OrganizationDO organizationDO) {
        organizationDOMapper.updateById(organizationDO);
    }

    @Override
    public void deleteOrganization(Long id) {
        organizationDOMapper.deleteById(id);
    }

    @Override
    public List<OrganizationDO> getRootOrganizations(Long tenantId) {
        LambdaQueryWrapper<OrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrganizationDO::getTenantId, tenantId).eq(OrganizationDO::getIsRoot, 1)
                .eq(OrganizationDO::getStatus, 1).orderByAsc(OrganizationDO::getSortOrder);
        return organizationDOMapper.selectList(queryWrapper);
    }

    // ========== 用户-组织关联方法实现 ==========

    @Override
    public List<Long> getOrganizationIdsByUserId(Long userId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getUserId, userId);
        List<UserOrganizationDO> list = userOrganizationDOMapper.selectList(queryWrapper);
        return list.stream().map(UserOrganizationDO::getOrganizationId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserIdsByOrganizationId(Long organizationId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getOrganizationId, organizationId);
        List<UserOrganizationDO> list = userOrganizationDOMapper.selectList(queryWrapper);
        return list.stream().map(UserOrganizationDO::getUserId).collect(Collectors.toList());
    }

    @Override
    public void addUserOrganization(UserOrganizationDO userOrganizationDO) {
        userOrganizationDOMapper.insert(userOrganizationDO);
    }

    @Override
    public void deleteUserOrganization(Long userId, Long organizationId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getUserId, userId)
                .eq(UserOrganizationDO::getOrganizationId, organizationId);
        userOrganizationDOMapper.delete(queryWrapper);
    }

    @Override
    public void deleteUserOrganizationsByUserId(Long userId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getUserId, userId);
        userOrganizationDOMapper.delete(queryWrapper);
    }

    @Override
    public List<UserOrganizationDO> getUserOrganizations(Long userId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getUserId, userId);
        return userOrganizationDOMapper.selectList(queryWrapper);
    }

    @Override
    public void updateUserOrganization(UserOrganizationDO userOrganizationDO) {
        userOrganizationDOMapper.updateById(userOrganizationDO);
    }

    @Override
    public boolean existsUserOrganization(Long userId, Long organizationId) {
        LambdaQueryWrapper<UserOrganizationDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOrganizationDO::getUserId, userId)
                .eq(UserOrganizationDO::getOrganizationId, organizationId);
        return userOrganizationDOMapper.selectCount(queryWrapper) > 0;
    }
}
