package com.tencent.supersonic.common.outbox;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.mybatis.TenantSqlInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSqlInterceptorOutboxExclusionTest {

    private TenantSqlInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantSqlInterceptor(new TenantConfig());
        TenantContext.setTenantId(99L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExcludeOutboxTables() throws Exception {
        Method m = TenantSqlInterceptor.class.getDeclaredMethod("shouldExcludeTable", String.class);
        m.setAccessible(true);
        assertThat((Boolean) m.invoke(interceptor, "s2_outbox")).isTrue();
        assertThat((Boolean) m.invoke(interceptor, "s2_outbox_dead")).isTrue();
    }

    @Test
    void tenantConfigExcludesOutboxTables() {
        TenantConfig config = new TenantConfig();
        assertThat(config.isExcludedTable("s2_outbox")).isTrue();
        assertThat(config.isExcludedTable("s2_outbox_dead")).isTrue();
    }
}
