package com.tencent.supersonic.common.quota;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    @Bean
    public TenantQuotaMeterBinder tenantQuotaMeterBinder(TenantQuotaService service) {
        return new TenantQuotaMeterBinder(service);
    }

    @Bean
    public FilterRegistrationBean<TenantQuotaFilter> tenantQuotaFilterRegistration() {
        FilterRegistrationBean<TenantQuotaFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TenantQuotaFilter());
        reg.addUrlPatterns("/api/*");
        reg.setName("tenantQuotaFilter");
        reg.setOrder(Integer.MIN_VALUE + 100);
        return reg;
    }
}
