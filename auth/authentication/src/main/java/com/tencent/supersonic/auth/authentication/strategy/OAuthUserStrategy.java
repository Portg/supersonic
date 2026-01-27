package com.tencent.supersonic.auth.authentication.strategy;

import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserSessionDO;
import com.tencent.supersonic.auth.authentication.session.SessionService;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * OAuth-based user strategy. This strategy supports both JWT tokens (from OAuth login) and
 * session-based authentication.
 */
@Slf4j
@Service
public class OAuthUserStrategy implements UserStrategy {

    public static final String STRATEGY_NAME = "oauth";
    private static final String SESSION_HEADER = "X-Session-Id";

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final OAuthConfig oauthConfig;

    public OAuthUserStrategy(TokenService tokenService, SessionService sessionService,
            OAuthConfig oauthConfig) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.oauthConfig = oauthConfig;
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean accept(boolean isEnableAuthentication) {
        return isEnableAuthentication && oauthConfig.isEnabled();
    }

    @Override
    public User findUser(HttpServletRequest request, HttpServletResponse response) {
        // First, try to find user from JWT token (standard flow after OAuth login)
        final Optional<Claims> claimsOptional = tokenService.getClaims(request);
        if (claimsOptional.isPresent()) {
            User user = getUserFromClaims(claimsOptional.get());
            if (user != null && user.getId() != null && user.getId() > 0) {
                // Optionally validate session
                String sessionId = getSessionId(request);
                if (sessionId != null && sessionService.isEnabled()) {
                    UserSessionDO session = sessionService.validateSession(sessionId);
                    if (session == null) {
                        log.debug("Session {} is invalid or expired", sessionId);
                        // Session invalid but token valid - still allow access
                    } else {
                        // Update session activity
                        sessionService.updateLastActivity(sessionId);
                    }
                }
                return user;
            }
        }

        // Return visit user if no valid authentication
        return User.getVisitUser();
    }

    @Override
    public User findUser(String token, String appKey) {
        final Optional<Claims> claimsOptional = tokenService.getClaims(token, appKey);
        return claimsOptional.map(this::getUserFromClaims).orElse(User.getVisitUser());
    }

    /**
     * Extract session ID from request header.
     */
    private String getSessionId(HttpServletRequest request) {
        return request.getHeader(SESSION_HEADER);
    }

    /**
     * Extract user from JWT claims.
     */
    private User getUserFromClaims(Claims claims) {
        try {
            Long userId =
                    Long.parseLong(claims.getOrDefault(UserConstants.TOKEN_USER_ID, 0).toString());
            String userName = String.valueOf(claims.get(UserConstants.TOKEN_USER_NAME));
            String email = String.valueOf(claims.get(UserConstants.TOKEN_USER_EMAIL));
            String displayName = String.valueOf(claims.get(UserConstants.TOKEN_USER_DISPLAY_NAME));
            Integer isAdmin = claims.get(UserConstants.TOKEN_IS_ADMIN) == null ? 0
                    : Integer.parseInt(claims.get(UserConstants.TOKEN_IS_ADMIN).toString());
            Long tenantId = claims.get(UserConstants.TOKEN_TENANT_ID) == null ? null
                    : Long.parseLong(claims.get(UserConstants.TOKEN_TENANT_ID).toString());
            String role = claims.get(UserConstants.TOKEN_USER_ROLE) == null ? null
                    : String.valueOf(claims.get(UserConstants.TOKEN_USER_ROLE));
            return User.get(userId, userName, displayName, email, isAdmin, tenantId, role);
        } catch (Exception e) {
            log.error("Failed to extract user from claims", e);
            return null;
        }
    }
}
