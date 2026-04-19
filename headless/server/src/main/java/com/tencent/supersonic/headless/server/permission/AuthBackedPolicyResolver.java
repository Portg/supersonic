package com.tencent.supersonic.headless.server.permission;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.PolicyResolver;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthBackedPolicyResolver implements PolicyResolver {

    private static final String DEFAULT_MASK = "CONCAT(LEFT(%s,3),'****')";

    private final AuthService authService;
    private final SchemaService schemaService;
    private final SensitiveLevelConfig sensitiveLevelConfig;

    @Override
    public List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds) {
        AuthorizedResourceResp auth = fetchAuth(user, modelIds);
        if (auth == null || CollectionUtils.isEmpty(auth.getFilters())) {
            return List.of();
        }
        List<RowPolicy> result = new ArrayList<>();
        for (DimensionFilter f : auth.getFilters()) {
            if (f.getExpressions() == null) {
                continue;
            }
            for (String expr : f.getExpressions()) {
                if (expr == null || expr.isBlank()) {
                    continue;
                }
                RowPolicy p = new RowPolicy();
                p.setPolicyId("row-" + UUID.nameUUIDFromBytes(expr.getBytes()));
                p.setModelId(modelIds.iterator().next());
                p.setTableBizNames(allTableBizNames(modelIds));
                p.setFilterExpression(expr);
                p.setDescription(f.getDescription());
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds) {
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null) {
            return List.of();
        }
        boolean includeMid = sensitiveLevelConfig.isMidLevelRequireAuth();
        AuthorizedResourceResp auth = fetchAuth(user, modelIds);
        Set<String> authedCols = auth == null || CollectionUtils.isEmpty(auth.getAuthResList())
                ? Set.of()
                : auth.getAuthResList().stream().map(AuthRes::getName).collect(Collectors.toSet());

        List<ColumnPolicy> out = new ArrayList<>();
        if (!CollectionUtils.isEmpty(schema.getDimensions())) {
            for (DimSchemaResp d : schema.getDimensions()) {
                if (shouldMask(d.getSensitiveLevel(), includeMid, d.getBizName(), authedCols)) {
                    out.add(new ColumnPolicy("col-" + d.getBizName(), modelIds.iterator().next(),
                            d.getBizName(), DEFAULT_MASK));
                }
            }
        }
        if (!CollectionUtils.isEmpty(schema.getMetrics())) {
            for (MetricSchemaResp m : schema.getMetrics()) {
                if (shouldMask(m.getSensitiveLevel(), includeMid, m.getBizName(), authedCols)) {
                    out.add(new ColumnPolicy("col-" + m.getBizName(), modelIds.iterator().next(),
                            m.getBizName(), DEFAULT_MASK));
                }
            }
        }
        return out;
    }

    private boolean shouldMask(Integer level, boolean includeMid, String bizName,
            Set<String> authed) {
        if (bizName == null || authed.contains(bizName)) {
            return false;
        }
        if (SensitiveLevelEnum.HIGH.getCode().equals(level)) {
            return true;
        }
        return includeMid && SensitiveLevelEnum.MID.getCode().equals(level);
    }

    private AuthorizedResourceResp fetchAuth(User user, Set<Long> modelIds) {
        try {
            QueryAuthResReq req = new QueryAuthResReq();
            req.setModelIds(new ArrayList<>(modelIds));
            return authService.queryAuthorizedResources(req, user);
        } catch (Exception e) {
            log.warn("auth fetch failed for user={} models={}", user.getName(), modelIds, e);
            return null;
        }
    }

    private SemanticSchemaResp fetchSchema(Set<Long> modelIds) {
        try {
            SchemaFilterReq f = new SchemaFilterReq();
            f.setModelIds(new ArrayList<>(modelIds));
            return schemaService.fetchSemanticSchema(f);
        } catch (Exception e) {
            log.warn("schema fetch failed for models={}", modelIds, e);
            return null;
        }
    }

    private List<String> allTableBizNames(Set<Long> modelIds) {
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null || schema.getModelResps() == null) {
            return List.of();
        }
        return schema.getModelResps().stream()
                .map(m -> m.getBizName() == null ? m.getName() : m.getBizName())
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.toList());
    }
}
