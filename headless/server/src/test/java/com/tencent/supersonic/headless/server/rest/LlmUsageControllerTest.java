package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import com.tencent.supersonic.common.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LlmUsageControllerTest {

    private static MockMvc buildMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter()).build();
    }

    private static UserService userService(User user) {
        UserService userService = mock(UserService.class);
        when(userService.getCurrentUser(any(), any())).thenReturn(user);
        return userService;
    }

    private static User platformAdmin() {
        return User.builder().id(1L).name("admin").tenantId(1L)
                .permissions(List.of("PLATFORM_ADMIN")).build();
    }

    @Test
    void queryReturnsPagedResults() throws Exception {
        LlmUsageService svc = mock(LlmUsageService.class);
        Page<LlmUsageDO> page = new Page<>(1, 20);
        LlmUsageDO row = new LlmUsageDO();
        row.setId(1L);
        row.setTenantId(7L);
        row.setModel("gpt-4o-mini");
        row.setTotalTokens(42);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(svc.query(eq(7L), any(), any(), isNull(), isNull(), eq(1), eq(20))).thenReturn(page);

        buildMvc(new LlmUsageController(svc, userService(platformAdmin())))
                .perform(get("/api/semantic/admin/llm-usage").param("tenantId", "7")
                        .param("from", "2026-04-01").param("to", "2026-04-17")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void dailyAggregatesEndpointReturnsTimeSeries() throws Exception {
        LlmUsageService svc = mock(LlmUsageService.class);
        when(svc.dailyAggregates(eq(7L), any(), any()))
                .thenReturn(List.of(Map.of("day", "2026-04-15", "tokens", 1000L, "cost", 123L)));

        buildMvc(new LlmUsageController(svc, userService(platformAdmin())))
                .perform(get("/api/semantic/admin/llm-usage/daily").param("tenantId", "7")
                        .param("from", "2026-04-01").param("to", "2026-04-17")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tokens").value(1000));
    }

    @Test
    void tenantUserCannotQueryOtherTenant() throws Exception {
        LlmUsageService svc = mock(LlmUsageService.class);
        User user = User.builder().id(2L).name("tenant-admin").tenantId(8L)
                .permissions(List.of("TENANT_USAGE_VIEW")).build();

        MockMvc mvc = buildMvc(new LlmUsageController(svc, userService(user)));
        assertThatThrownBy(() -> mvc.perform(get("/api/semantic/admin/llm-usage")
                .param("tenantId", "7").accept(MediaType.APPLICATION_JSON)))
                        .hasRootCauseInstanceOf(IllegalAccessException.class);
        verifyNoInteractions(svc);
    }
}
