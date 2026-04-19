package com.tencent.supersonic.headless.server.permission;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthBackedPolicyResolverTest {

    private AuthService authService;
    private SchemaService schemaService;
    private SensitiveLevelConfig sensitiveLevelConfig;
    private com.tencent.supersonic.headless.server.service.DataSetAuthService dataSetAuthService;
    private AuthBackedPolicyResolver resolver;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        schemaService = mock(SchemaService.class);
        sensitiveLevelConfig = mock(SensitiveLevelConfig.class);
        dataSetAuthService =
                mock(com.tencent.supersonic.headless.server.service.DataSetAuthService.class);
        resolver = new AuthBackedPolicyResolver(authService, schemaService, sensitiveLevelConfig,
                dataSetAuthService);
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
        ModelResp model = new ModelResp();
        model.setBizName("s2_order");
        schema.setModelResps(List.of(model));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);

        List<RowPolicy> out = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L), null);
        assertEquals(1, out.size());
        assertEquals("region = 'APAC'", out.get(0).getFilterExpression());
        assertEquals("APAC only", out.get(0).getDescription());
        assertEquals(List.of("s2_order"), out.get(0).getTableBizNames());
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

        List<ColumnPolicy> out =
                resolver.resolveColumnPolicies(User.get(0L, "alice"), Set.of(1L), null);
        assertTrue(out.stream().anyMatch(cp -> "phone".equals(cp.getColumnBizName())));
    }

    @Test
    void mergesDatasetFiltersIntoRowPolicies() {
        AuthorizedResourceResp modelResp = new AuthorizedResourceResp();
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenReturn(modelResp);

        AuthorizedResourceResp dataSetResp = new AuthorizedResourceResp();
        DimensionFilter filter = new DimensionFilter();
        filter.setExpressions(new ArrayList<>(List.of("tenant_id = 7")));
        dataSetResp.setFilters(new ArrayList<>(List.of(filter)));
        when(dataSetAuthService.queryAuthorizedResources(eq(99L), any(User.class)))
                .thenReturn(dataSetResp);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        ModelResp model = new ModelResp();
        model.setBizName("s2_order");
        schema.setModelResps(List.of(model));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);

        List<RowPolicy> out = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L), 99L);
        assertEquals(1, out.size());
        assertEquals("tenant_id = 7", out.get(0).getFilterExpression());
    }

    @Test
    void authFailureFailsClosed() {
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenThrow(new RuntimeException("auth down"));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L), null));
    }
}
