package com.tencent.supersonic.headless.server.permission;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthBackedPolicyResolverTest {

    private AuthService authService;
    private SchemaService schemaService;
    private SensitiveLevelConfig sensitiveLevelConfig;
    private AuthBackedPolicyResolver resolver;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        schemaService = mock(SchemaService.class);
        sensitiveLevelConfig = mock(SensitiveLevelConfig.class);
        resolver = new AuthBackedPolicyResolver(authService, schemaService, sensitiveLevelConfig);
    }

    @Test
    void mapsDimensionFiltersToRowPolicies() {
        AuthorizedResourceResp resp = new AuthorizedResourceResp();
        DimensionFilter f = new DimensionFilter();
        f.setExpressions(new ArrayList<>(List.of("region = 'APAC'")));
        f.setDescription("APAC only");
        resp.setFilters(new ArrayList<>(List.of(f)));
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenReturn(resp);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        schema.setModelIds(List.of(1L));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);

        List<RowPolicy> out = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L));
        assertEquals(1, out.size());
        assertEquals("region = 'APAC'", out.get(0).getFilterExpression());
        assertEquals("APAC only", out.get(0).getDescription());
    }

    @Test
    void mapsHighSensitiveColumnsWithDefaultMaskToColumnPolicies() {
        AuthorizedResourceResp resp = new AuthorizedResourceResp();
        resp.setAuthResList(new ArrayList<>());
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenReturn(resp);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        DimSchemaResp phone = new DimSchemaResp();
        phone.setBizName("phone");
        phone.setSensitiveLevel(2); // HIGH
        schema.setDimensions(new ArrayList<>(List.of(phone)));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);
        when(sensitiveLevelConfig.isMidLevelRequireAuth()).thenReturn(false);

        List<ColumnPolicy> out = resolver.resolveColumnPolicies(User.get(0L, "alice"), Set.of(1L));
        assertTrue(out.stream().anyMatch(cp -> "phone".equals(cp.getColumnBizName())));
    }
}
