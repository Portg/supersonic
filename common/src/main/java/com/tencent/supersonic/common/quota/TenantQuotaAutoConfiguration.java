package com.tencent.supersonic.common.quota;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantQuotaConfig.class)
@ConditionalOnProperty(prefix = "s2.tenant.quota", name = "enabled", havingValue = "true")
public class TenantQuotaAutoConfiguration {

    @Bean
    public TenantQuotaService tenantQuotaService(TenantQuotaConfig config,
            ObjectProvider<Function<Long, TenantQuotaOverride>> loaderProvider) {
        Function<Long, TenantQuotaOverride> loader =
                loaderProvider.getIfAvailable(() -> tid -> null);
        return new InMemoryTenantQuotaService(config, loader);
    }
}
