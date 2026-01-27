package com.tencent.supersonic.auth.authentication.config;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Configuration class that provides a user resolver function for tenant identification. This allows
 * the TenantInterceptor to extract tenant ID from the authenticated user's JWT token.
 */
@Configuration
public class TenantUserResolverConfig {

    @Autowired
    private UserService userService;

    /**
     * Creates a function that resolves the current user from an HTTP request. This function is used
     * by TenantInterceptor to get the tenant ID from the authenticated user.
     */
    @Bean
    public Function<HttpServletRequest, User> tenantUserResolver() {
        return request -> {
            try {
                return userService.getCurrentUser(request, null);
            } catch (Exception e) {
                return null;
            }
        };
    }
}
