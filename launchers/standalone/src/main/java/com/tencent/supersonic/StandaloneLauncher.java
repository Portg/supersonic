package com.tencent.supersonic;

import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.api.authentication.config.RefreshTokenConfig;
import com.tencent.supersonic.auth.api.authentication.config.SessionConfig;
import com.tencent.supersonic.common.config.TenantConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic", "dev.langchain4j"},
        exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableConfigurationProperties({TenantConfig.class, OAuthConfig.class, SessionConfig.class,
                RefreshTokenConfig.class})
@EnableScheduling
@EnableAsync
public class StandaloneLauncher {

    public static void main(String[] args) {
        SpringApplication.run(StandaloneLauncher.class, args);
    }
}
