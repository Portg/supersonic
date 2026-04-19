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
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
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
    private final DataSetAuthService dataSetAuthService;

    @Override
    public List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds, Long dataSetId) {
        AuthorizedResourceResp auth = fetchAuth(user, modelIds, dataSetId);
        if (CollectionUtils.isEmpty(auth.getFilters())) {
            return List.of();
        }
        List<String> tableBizNames = allTableBizNames(modelIds);
        if (tableBizNames.isEmpty()) {
            throw new IllegalStateException(
                    "failed to resolve policy table names for models=" + modelIds);
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
                p.setTableBizNames(tableBizNames);
                p.setFilterExpression(expr);
                p.setDescription(f.getDescription());
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds, Long dataSetId) {
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null) {
            throw new IllegalStateException("failed to resolve semantic schema for policies");
        }
        boolean includeMid = sensitiveLevelConfig.isMidLevelRequireAuth();
        AuthorizedResourceResp auth = fetchAuth(user, modelIds, dataSetId);
        Set<String> authedCols = CollectionUtils.isEmpty(auth.getAuthResList()) ? Set.of()
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

    private AuthorizedResourceResp fetchAuth(User user, Set<Long> modelIds, Long dataSetId) {
        try {
            QueryAuthResReq req = new QueryAuthResReq();
            req.setModelIds(new ArrayList<>(modelIds));
            AuthorizedResourceResp authorizedResource =
                    authService.queryAuthorizedResources(req, user);
            if (authorizedResource == null) {
                authorizedResource = new AuthorizedResourceResp();
            }
            AuthorizedResourceResp merged = new AuthorizedResourceResp();
            mergeAuthorizedResource(merged, authorizedResource);
            if (dataSetId != null) {
                AuthorizedResourceResp dataSetAuthResource =
                        dataSetAuthService.queryAuthorizedResources(dataSetId, user);
                if (dataSetAuthResource != null) {
                    mergeAuthorizedResource(merged, dataSetAuthResource);
                }
            }
            return merged;
        } catch (Exception e) {
            log.warn("auth fetch failed for user={} models={}", user.getName(), modelIds, e);
            throw new IllegalStateException("failed to resolve authorization policies", e);
        }
    }

    private SemanticSchemaResp fetchSchema(Set<Long> modelIds) {
        try {
            SchemaFilterReq f = new SchemaFilterReq();
            f.setModelIds(new ArrayList<>(modelIds));
            return schemaService.fetchSemanticSchema(f);
        } catch (Exception e) {
            log.warn("schema fetch failed for models={}", modelIds, e);
            throw new IllegalStateException("failed to resolve semantic schema for policies", e);
        }
    }

    private void mergeAuthorizedResource(AuthorizedResourceResp target,
            AuthorizedResourceResp source) {
        if (!CollectionUtils.isEmpty(source.getAuthResList())) {
            target.getAuthResList().addAll(source.getAuthResList());
        }
        if (!CollectionUtils.isEmpty(source.getFilters())) {
            target.getFilters().addAll(source.getFilters());
        }
    }

    private List<String> allTableBizNames(Set<Long> modelIds) {
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null || schema.getModelResps() == null) {
            return List.of();
        }
        return schema.getModelResps().stream().flatMap(m -> {
            List<String> names = new ArrayList<>();
            names.add(m.getBizName());
            // m.getAlias() is a comma-separated string of SQL aliases, not a single identifier
            if (m.getAlias() != null) {
                for (String a : m.getAlias().split(",")) {
                    names.add(a.trim());
                }
            }
            // fullPath format: "db.schema.table" — include full path and leaf name only
            if (m.getFullPath() != null) {
                names.add(m.getFullPath());
                if (m.getFullPath().contains(".")) {
                    names.add(m.getFullPath().substring(m.getFullPath().lastIndexOf('.') + 1));
                }
            }
            return names.stream();
        }).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
    }
}
