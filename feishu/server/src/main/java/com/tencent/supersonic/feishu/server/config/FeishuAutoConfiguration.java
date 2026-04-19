package com.tencent.supersonic.feishu.server.config;

import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "s2.feishu", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FeishuProperties.class)
@ComponentScan(basePackages = "com.tencent.supersonic.feishu.server")
public class FeishuAutoConfiguration {
    // Cache backends provided by UnifiedCacheAutoConfiguration.
    // See s2.cache.namespaces.feishu-{event-dedup,token,general,counter}.
}
