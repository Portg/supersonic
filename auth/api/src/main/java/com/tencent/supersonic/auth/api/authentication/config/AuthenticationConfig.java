package com.tencent.supersonic.auth.api.authentication.config;

import lombok.Data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authentication configuration properties. Plain POJO — Spring registration is handled by
 * AuthenticationConfigFactory in auth-authentication (keeps auth-api free of Spring machinery).
 */
@Data
public class AuthenticationConfig {

    private String excludePath = "XXX";
    private String includePath = "/api";
    private String strategy = "http";
    private boolean enabled = false;
    private String tokenDefaultAppKey = "supersonic";
    private String tokenAppSecret =
            "supersonic:WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==";
    private String tokenHttpHeaderKey = "Authorization";
    private String tokenHttpHeaderAppKey = "App-Key";
    private String appId = "appId";
    private String timestamp = "timestamp";
    private String signature = "signature";
    private Long tokenTimeout = 72000000L;

    public Map<String, String> getAppKeyToSecretMap() {
        return Arrays.stream(this.tokenAppSecret.split(",")).map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }
}
