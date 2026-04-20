package com.tencent.supersonic.auth.authentication.config;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers AuthenticationConfig as a Spring bean. The POJO lives in auth-api (contract module);
 * 
 * @Value binding happens here in the implementation module.
 */
@Configuration
public class AuthenticationConfigFactory {

    @Bean
    public AuthenticationConfig authenticationConfig(
            @Value("${s2.authentication.exclude.path:XXX}") String excludePath,
            @Value("${s2.authentication.include.path:/api}") String includePath,
            @Value("${s2.authentication.strategy:http}") String strategy,
            @Value("${s2.authentication.enable:false}") boolean enabled,
            @Value("${s2.authentication.token.default.appKey:supersonic}") String tokenDefaultAppKey,
            @Value("${s2.authentication.token.appSecret:"
                    + "supersonic:WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==}") String tokenAppSecret,
            @Value("${s2.authentication.token.http.header.key:Authorization}") String tokenHttpHeaderKey,
            @Value("${s2.authentication.token.http.app.key:App-Key}") String tokenHttpHeaderAppKey,
            @Value("${s2.authentication.app.appId:appId}") String appId,
            @Value("${s2.authentication.app.timestamp:timestamp}") String timestamp,
            @Value("${s2.authentication.app.signature:signature}") String signature,
            @Value("${s2.authentication.token.timeout:72000000}") Long tokenTimeout) {
        AuthenticationConfig config = new AuthenticationConfig();
        config.setExcludePath(excludePath);
        config.setIncludePath(includePath);
        config.setStrategy(strategy);
        config.setEnabled(enabled);
        config.setTokenDefaultAppKey(tokenDefaultAppKey);
        config.setTokenAppSecret(tokenAppSecret);
        config.setTokenHttpHeaderKey(tokenHttpHeaderKey);
        config.setTokenHttpHeaderAppKey(tokenHttpHeaderAppKey);
        config.setAppId(appId);
        config.setTimestamp(timestamp);
        config.setSignature(signature);
        config.setTokenTimeout(tokenTimeout);
        return config;
    }
}
