package com.tencent.supersonic.auth.authorization.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.BatchAuthorizeReq;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.response.BatchOperationResult;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.auth.authorization.persistence.dataobject.AuthGroupDO;
import com.tencent.supersonic.auth.authorization.persistence.mapper.AuthGroupDOMapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthChangeType;
import com.tencent.supersonic.common.service.AuthAuditService;
import com.tencent.supersonic.common.util.RowFilterValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthGroupDOMapper authGroupDOMapper;
    private final UserService userService;
    private final AuthAuditService authAuditService;
    private final Gson gson = new Gson();

    public AuthServiceImpl(AuthGroupDOMapper authGroupDOMapper, UserService userService,
            AuthAuditService authAuditService) {
        this.authGroupDOMapper = authGroupDOMapper;
        this.userService = userService;
        this.authAuditService = authAuditService;
    }

    private List<AuthGroup> load() {
        List<AuthGroupDO> rows = authGroupDOMapper.selectList(null);
        return rows.stream().map(row -> gson.fromJson(row.getConfig(), AuthGroup.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthGroup> queryAuthGroups(String modelId, Integer groupId) {
        return load().stream()
                .filter(group -> (Objects.isNull(groupId) || groupId.equals(group.getGroupId()))
                        && modelId.equals(group.getModelId().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public void addOrUpdateAuthGroup(AuthGroup group) {
        // Validate row filter expressions for SQL injection
        validateDimensionFilters(group.getDimensionFilters());

        if (group.getGroupId() == null) {
            // Get next group ID
            Integer maxGroupId = getMaxGroupId();
            int nextGroupId = (maxGroupId == null) ? 1 : maxGroupId + 1;
            group.setGroupId(nextGroupId);

            AuthGroupDO authGroupDO = new AuthGroupDO();
            authGroupDO.setGroupId(nextGroupId);
            authGroupDO.setConfig(gson.toJson(group));
            authGroupDOMapper.insert(authGroupDO);

            // Audit log for create
            authAuditService.logAuthChange(AuthChangeType.CREATE, "MODEL", group.getModelId(),
                    nextGroupId, "system", null, gson.toJson(group),
                    "Created auth group: " + group.getName());
        } else {
            // Get old value for audit
            AuthGroupDO oldGroupDO = authGroupDOMapper.selectById(group.getGroupId());
            String oldValue = oldGroupDO != null ? oldGroupDO.getConfig() : null;

            AuthGroupDO authGroupDO = new AuthGroupDO();
            authGroupDO.setGroupId(group.getGroupId());
            authGroupDO.setConfig(gson.toJson(group));
            authGroupDOMapper.updateById(authGroupDO);

            // Audit log for update
            authAuditService.logAuthChange(AuthChangeType.UPDATE, "MODEL", group.getModelId(),
                    group.getGroupId(), "system", oldValue, gson.toJson(group),
                    "Updated auth group: " + group.getName());
        }
    }

    /**
     * Validates dimension filter expressions to prevent SQL injection attacks.
     */
    private void validateDimensionFilters(List<String> dimensionFilters) {
        if (CollectionUtils.isEmpty(dimensionFilters)) {
            return;
        }
        for (String filter : dimensionFilters) {
            RowFilterValidator.ValidationResult result = RowFilterValidator.validate(filter);
            if (!result.isValid()) {
                throw new RuntimeException(
                        "Invalid row filter expression: " + result.getErrorMessage());
            }
        }
    }

    private Integer getMaxGroupId() {
        LambdaQueryWrapper<AuthGroupDO> query = new LambdaQueryWrapper<>();
        query.orderByDesc(AuthGroupDO::getGroupId).last("LIMIT 1");
        AuthGroupDO maxGroup = authGroupDOMapper.selectOne(query);
        return maxGroup != null ? maxGroup.getGroupId() : null;
    }

    @Override
    public void removeAuthGroup(AuthGroup group) {
        // Get old value for audit
        AuthGroupDO oldGroupDO = authGroupDOMapper.selectById(group.getGroupId());
        String oldValue = oldGroupDO != null ? oldGroupDO.getConfig() : null;

        authGroupDOMapper.deleteById(group.getGroupId());

        // Audit log for delete
        authAuditService.logAuthChange(AuthChangeType.DELETE, "MODEL", group.getModelId(),
                group.getGroupId(), "system", oldValue, null,
                "Deleted auth group: " + group.getName());
    }

    @Override
    public AuthorizedResourceResp queryAuthorizedResources(QueryAuthResReq req, User user) {
        if (CollectionUtils.isEmpty(req.getModelIds())) {
            return new AuthorizedResourceResp();
        }
        Set<String> userOrgIds = userService.getUserAllOrgId(user.getName());
        List<AuthGroup> groups =
                getAuthGroups(req.getModelIds(), user.getName(), new ArrayList<>(userOrgIds));
        AuthorizedResourceResp resource = new AuthorizedResourceResp();
        Map<Long, List<AuthGroup>> authGroupsByModelId =
                groups.stream().collect(Collectors.groupingBy(AuthGroup::getModelId));
        for (Long modelId : req.getModelIds()) {
            if (authGroupsByModelId.containsKey(modelId)) {
                List<AuthGroup> authGroups = authGroupsByModelId.get(modelId);
                for (AuthGroup authRuleGroup : authGroups) {
                    List<AuthRule> authRules = authRuleGroup.getAuthRules();
                    for (AuthRule authRule : authRules) {
                        for (String resBizName : authRule.resourceNames()) {
                            resource.getAuthResList().add(new AuthRes(modelId, resBizName));
                        }
                    }
                }
            }
        }
        Set<Map.Entry<Long, List<AuthGroup>>> entries = authGroupsByModelId.entrySet();
        for (Map.Entry<Long, List<AuthGroup>> entry : entries) {
            List<AuthGroup> authGroups = entry.getValue();
            for (AuthGroup authGroup : authGroups) {
                DimensionFilter df = new DimensionFilter();
                df.setDescription(authGroup.getDimensionFilterDescription());
                df.setExpressions(authGroup.getDimensionFilters());
                resource.getFilters().add(df);
            }
        }
        return resource;
    }

    private List<AuthGroup> getAuthGroups(List<Long> modelIds, String userName,
            List<String> departmentIds) {
        List<AuthGroup> groups = load().stream().filter(group -> {
            if (!modelIds.contains(group.getModelId())) {
                return false;
            }
            if (!CollectionUtils.isEmpty(group.getAuthorizedUsers())
                    && group.getAuthorizedUsers().contains(userName)) {
                return true;
            }
            for (String departmentId : departmentIds) {
                if (!CollectionUtils.isEmpty(group.getAuthorizedDepartmentIds())
                        && group.getAuthorizedDepartmentIds().contains(departmentId)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        log.info("user:{} department:{} authGroups:{}", userName, departmentIds, groups);
        return groups;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchCreateAuthGroups(List<AuthGroup> groups) {
        BatchOperationResult result = new BatchOperationResult();
        if (CollectionUtils.isEmpty(groups)) {
            return result;
        }
        Integer maxGroupId = getMaxGroupId();
        int nextGroupId = (maxGroupId == null) ? 1 : maxGroupId + 1;

        for (AuthGroup group : groups) {
            try {
                group.setGroupId(nextGroupId);
                AuthGroupDO authGroupDO = new AuthGroupDO();
                authGroupDO.setGroupId(nextGroupId);
                authGroupDO.setConfig(gson.toJson(group));
                authGroupDOMapper.insert(authGroupDO);
                result.addSuccess(nextGroupId);
                nextGroupId++;
            } catch (Exception e) {
                log.error("Failed to create auth group: {}", group.getName(), e);
                result.addFail(nextGroupId, e.getMessage());
                nextGroupId++;
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchUpdateAuthGroups(List<AuthGroup> groups) {
        BatchOperationResult result = new BatchOperationResult();
        if (CollectionUtils.isEmpty(groups)) {
            return result;
        }

        for (AuthGroup group : groups) {
            try {
                if (group.getGroupId() == null) {
                    result.addFail(null, "groupId is required for update");
                    continue;
                }
                AuthGroupDO authGroupDO = new AuthGroupDO();
                authGroupDO.setGroupId(group.getGroupId());
                authGroupDO.setConfig(gson.toJson(group));
                authGroupDOMapper.updateById(authGroupDO);
                result.addSuccess(group.getGroupId());
            } catch (Exception e) {
                log.error("Failed to update auth group: {}", group.getGroupId(), e);
                result.addFail(group.getGroupId(), e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchRemoveAuthGroups(List<Integer> groupIds) {
        BatchOperationResult result = new BatchOperationResult();
        if (CollectionUtils.isEmpty(groupIds)) {
            return result;
        }

        for (Integer groupId : groupIds) {
            try {
                authGroupDOMapper.deleteById(groupId);
                result.addSuccess(groupId);
            } catch (Exception e) {
                log.error("Failed to remove auth group: {}", groupId, e);
                result.addFail(groupId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchAuthorize(BatchAuthorizeReq req) {
        BatchOperationResult result = new BatchOperationResult();
        if (CollectionUtils.isEmpty(req.getGroupIds())) {
            return result;
        }

        for (Integer groupId : req.getGroupIds()) {
            try {
                AuthGroupDO authGroupDO = authGroupDOMapper.selectById(groupId);
                if (authGroupDO == null) {
                    result.addFail(groupId, "AuthGroup not found");
                    continue;
                }

                AuthGroup group = gson.fromJson(authGroupDO.getConfig(), AuthGroup.class);
                if (!CollectionUtils.isEmpty(req.getUsers())) {
                    List<String> users = group.getAuthorizedUsers();
                    if (users == null) {
                        users = new ArrayList<>();
                    }
                    for (String user : req.getUsers()) {
                        if (!users.contains(user)) {
                            users.add(user);
                        }
                    }
                    group.setAuthorizedUsers(users);
                }

                if (!CollectionUtils.isEmpty(req.getDepartmentIds())) {
                    List<String> deptIds = group.getAuthorizedDepartmentIds();
                    if (deptIds == null) {
                        deptIds = new ArrayList<>();
                    }
                    for (String deptId : req.getDepartmentIds()) {
                        if (!deptIds.contains(deptId)) {
                            deptIds.add(deptId);
                        }
                    }
                    group.setAuthorizedDepartmentIds(deptIds);
                }

                authGroupDO.setConfig(gson.toJson(group));
                authGroupDOMapper.updateById(authGroupDO);
                result.addSuccess(groupId);
            } catch (Exception e) {
                log.error("Failed to batch authorize group: {}", groupId, e);
                result.addFail(groupId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchRevokeAuthorize(BatchAuthorizeReq req) {
        BatchOperationResult result = new BatchOperationResult();
        if (CollectionUtils.isEmpty(req.getGroupIds())) {
            return result;
        }

        for (Integer groupId : req.getGroupIds()) {
            try {
                AuthGroupDO authGroupDO = authGroupDOMapper.selectById(groupId);
                if (authGroupDO == null) {
                    result.addFail(groupId, "AuthGroup not found");
                    continue;
                }

                AuthGroup group = gson.fromJson(authGroupDO.getConfig(), AuthGroup.class);
                if (!CollectionUtils.isEmpty(req.getUsers())
                        && group.getAuthorizedUsers() != null) {
                    group.getAuthorizedUsers().removeAll(req.getUsers());
                }

                if (!CollectionUtils.isEmpty(req.getDepartmentIds())
                        && group.getAuthorizedDepartmentIds() != null) {
                    group.getAuthorizedDepartmentIds().removeAll(req.getDepartmentIds());
                }

                authGroupDO.setConfig(gson.toJson(group));
                authGroupDOMapper.updateById(authGroupDO);
                result.addSuccess(groupId);
            } catch (Exception e) {
                log.error("Failed to batch revoke authorize group: {}", groupId, e);
                result.addFail(groupId, e.getMessage());
            }
        }
        return result;
    }
}
