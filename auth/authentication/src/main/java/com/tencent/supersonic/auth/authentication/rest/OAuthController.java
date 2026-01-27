package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import com.tencent.supersonic.auth.authentication.oauth.provider.OAuthException;
import com.tencent.supersonic.auth.authentication.oauth.service.OAuthCodeExchangeService;
import com.tencent.supersonic.auth.authentication.oauth.service.OAuthService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserSessionDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.refresh.RefreshTokenService;
import com.tencent.supersonic.auth.authentication.session.SessionService;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth authentication controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class OAuthController {

    private static final String EXCHANGE_CODE_COOKIE = "oauth_exchange_code";
    private static final int EXCHANGE_CODE_COOKIE_MAX_AGE = 30; // 30 seconds

    private final OAuthService oauthService;
    private final OAuthConfig oauthConfig;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final OAuthCodeExchangeService codeExchangeService;

    public OAuthController(OAuthService oauthService, OAuthConfig oauthConfig,
            TokenService tokenService, SessionService sessionService,
            RefreshTokenService refreshTokenService, UserRepository userRepository,
            OAuthCodeExchangeService codeExchangeService) {
        this.oauthService = oauthService;
        this.oauthConfig = oauthConfig;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.codeExchangeService = codeExchangeService;
    }

    /**
     * Get list of available OAuth providers.
     */
    @AuthenticationIgnore
    @GetMapping("/oauth/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", oauthService.isEnabled());
        response.put("providers", oauthService.getAvailableProviders());
        response.put("defaultProvider", oauthConfig.getDefaultProvider());
        return ResponseEntity.ok(response);
    }

    /**
     * Start OAuth authorization flow.
     */
    @AuthenticationIgnore
    @GetMapping("/oauth/authorize/{provider}")
    public void authorize(@PathVariable String provider, HttpServletResponse response)
            throws IOException {
        try {
            String authUrl = oauthService.startAuthorization(provider);
            response.sendRedirect(authUrl);
        } catch (OAuthException e) {
            log.error("Failed to start OAuth authorization: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getErrorDescription());
        }
    }

    /**
     * Handle OAuth callback.
     */
    @AuthenticationIgnore
    @GetMapping("/oauth/callback/{provider}")
    public void callback(@PathVariable String provider, @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Check for error from OAuth provider
        if (error != null) {
            log.error("OAuth error from provider {}: {} - {}", provider, error, errorDescription);
            response.sendRedirect("/webapp/#/login?error=" + error);
            return;
        }

        if (code == null || state == null) {
            log.error("OAuth callback missing code or state");
            response.sendRedirect("/webapp/#/login?error=missing_params");
            return;
        }

        try {
            OAuthService.OAuthCallbackResult result =
                    oauthService.handleCallback(provider, code, state);
            OAuthUserInfo userInfo = result.getUserInfo();

            // Find or create user
            UserDO user = findOrCreateUser(userInfo);

            // Store OAuth tokens
            oauthService.storeTokens(user.getId(), provider, result.getTokenResponse());

            // Create session
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            UserSessionDO session = sessionService.createSession(user.getId(), "OAUTH", provider,
                    ipAddress, userAgent);

            // Generate local JWT token
            UserWithPassword userWithPassword = convertToUserWithPassword(user);
            Map<String, Object> claims = UserWithPassword.convert(userWithPassword);

            // Add session ID to claims if session was created
            if (session != null) {
                claims.put("session_id", session.getSessionId());
            }

            String token = tokenService.generateToken(claims, tokenService.getTokenTimeout());

            // Generate refresh token
            String refreshToken = null;
            if (refreshTokenService.isEnabled() && session != null) {
                RefreshTokenService.RefreshTokenResult refreshResult = refreshTokenService
                        .generateRefreshToken(user.getId(), session.getId(), userAgent, ipAddress);
                if (refreshResult != null) {
                    refreshToken = refreshResult.getRawToken();
                }
            }

            // Create exchange code for secure token retrieval
            String sessionId = session != null ? session.getSessionId() : null;
            String exchangeCode = codeExchangeService.createExchangeCode(token, refreshToken,
                    sessionId, user.getId());

            // Set exchange code in HTTP-only cookie
            setExchangeCodeCookie(response, exchangeCode);

            // Redirect to frontend callback page (without tokens in URL)
            response.sendRedirect("/webapp/#/login/callback");

        } catch (OAuthException e) {
            log.error("OAuth callback failed: {}", e.getMessage());
            response.sendRedirect("/webapp/#/login?error=" + e.getErrorCode());
        }
    }

    /**
     * Link OAuth provider to existing account.
     */
    @PostMapping("/oauth/link/{provider}")
    public ResponseEntity<Map<String, Object>> linkProvider(@PathVariable String provider,
            HttpServletResponse response) {
        try {
            String authUrl = oauthService.startAuthorization(provider);
            Map<String, Object> result = new HashMap<>();
            result.put("authorizationUrl", authUrl);
            return ResponseEntity.ok(result);
        } catch (OAuthException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getErrorCode(), "message", e.getErrorDescription()));
        }
    }

    /**
     * Unlink OAuth provider from account.
     */
    @DeleteMapping("/oauth/unlink/{provider}")
    public ResponseEntity<Map<String, Object>> unlinkProvider(@PathVariable String provider,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = UserHolder.findUser(request, response);
        if (currentUser == null || currentUser.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            oauthService.unlinkProvider(currentUser.getId(), provider);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (OAuthException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getErrorCode(), "message", e.getErrorDescription()));
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    @AuthenticationIgnore
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String refreshToken = body.get("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "invalid_request", "message", "refresh_token is required"));
        }

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        RefreshTokenService.RefreshResult result =
                refreshTokenService.refresh(refreshToken, userAgent, ipAddress);
        if (result == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "message",
                    "Invalid or expired refresh token"));
        }

        // Get user
        UserDO user = userRepository.getUser(result.getUserId());
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "user_not_found", "message", "User not found"));
        }

        // Generate new access token
        UserWithPassword userWithPassword = convertToUserWithPassword(user);
        Map<String, Object> claims = UserWithPassword.convert(userWithPassword);
        String newAccessToken = tokenService.generateToken(claims, tokenService.getTokenTimeout());

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", newAccessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", tokenService.getTokenTimeout() / 1000);

        if (result.getNewRefreshToken() != null) {
            response.put("refresh_token", result.getNewRefreshToken().getRawToken());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke refresh token.
     */
    @PostMapping("/token/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "invalid_request", "message", "refresh_token is required"));
        }

        boolean revoked = refreshTokenService.revokeToken(refreshToken);
        return ResponseEntity.ok(Map.of("success", revoked));
    }

    /**
     * Get active sessions for current user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(HttpServletRequest request,
            HttpServletResponse response) {
        User currentUser = UserHolder.findUser(request, response);
        if (currentUser == null || currentUser.getId() == null) {
            return ResponseEntity.status(401).body(null);
        }

        List<UserSessionDO> sessions = sessionService.getActiveSessionsForUser(currentUser.getId());

        List<Map<String, Object>> sessionList = sessions.stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getSessionId());
            map.put("authMethod", session.getAuthMethod());
            map.put("providerName", session.getProviderName());
            map.put("createdAt", session.getCreatedAt());
            map.put("lastActivityAt", session.getLastActivityAt());
            map.put("ipAddress", session.getIpAddress());
            map.put("userAgent", session.getUserAgent());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(sessionList);
    }

    /**
     * Revoke a specific session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> revokeSession(@PathVariable String sessionId,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = UserHolder.findUser(request, response);
        if (currentUser == null || currentUser.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Verify the session belongs to the current user
        UserSessionDO session = sessionService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
        }

        boolean revoked = sessionService.revokeSession(sessionId, "User requested");
        return ResponseEntity.ok(Map.of("success", revoked));
    }

    /**
     * Revoke all sessions except current.
     */
    @DeleteMapping("/sessions/all")
    public ResponseEntity<Map<String, Object>> revokeAllSessions(
            @RequestHeader(value = "X-Session-Id", required = false) String currentSessionId,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = UserHolder.findUser(request, response);
        if (currentUser == null || currentUser.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        int revoked = sessionService.revokeAllSessionsForUser(currentUser.getId(),
                "User requested logout from all devices", currentSessionId);

        return ResponseEntity.ok(Map.of("revokedCount", revoked));
    }

    /**
     * Find or create a user from OAuth user info.
     */
    private UserDO findOrCreateUser(OAuthUserInfo userInfo) {
        // Try to find existing user by email
        String email = userInfo.getEmail();
        if (email != null && !email.isEmpty()) {
            List<UserDO> users = userRepository.getUserList();
            for (UserDO user : users) {
                if (email.equalsIgnoreCase(user.getEmail())) {
                    // Update last login
                    user.setLastLogin(new Timestamp(System.currentTimeMillis()));
                    userRepository.updateUser(user);
                    return user;
                }
            }
        }

        // Create new user
        UserDO newUser = new UserDO();
        newUser.setName(userInfo.getUsername());
        newUser.setDisplayName(userInfo.getDisplayName());
        newUser.setEmail(userInfo.getEmail());
        newUser.setIsAdmin(0);
        newUser.setLastLogin(new Timestamp(System.currentTimeMillis()));

        userRepository.addUser(newUser);
        log.info("Created new user from OAuth: {}", newUser.getName());

        return newUser;
    }

    /**
     * Convert UserDO to UserWithPassword.
     */
    private UserWithPassword convertToUserWithPassword(UserDO user) {
        return UserWithPassword.get(user.getId(), user.getName(), user.getDisplayName(),
                user.getEmail(), user.getPassword(), user.getIsAdmin());
    }

    /**
     * Exchange the one-time code for tokens. This endpoint should be called by the frontend after
     * OAuth callback redirect. The exchange code is retrieved from the HTTP-only cookie.
     */
    @AuthenticationIgnore
    @PostMapping("/oauth/token/exchange")
    public ResponseEntity<Map<String, Object>> exchangeToken(HttpServletRequest request,
            HttpServletResponse response) {

        // Get exchange code from cookie
        String exchangeCode = getExchangeCodeFromCookie(request);
        if (exchangeCode == null || exchangeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_request", "message", "Exchange code not found"));
        }

        // Exchange code for tokens
        OAuthCodeExchange exchange = codeExchangeService.exchangeCodeForTokens(exchangeCode);
        if (exchange == null) {
            // Clear the invalid cookie
            clearExchangeCodeCookie(response);
            return ResponseEntity.status(401).body(
                    Map.of("error", "invalid_code", "message", "Invalid or expired exchange code"));
        }

        // Clear the cookie after successful exchange
        clearExchangeCodeCookie(response);

        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", exchange.getAccessToken());
        result.put("token_type", "Bearer");
        result.put("expires_in", tokenService.getTokenTimeout() / 1000);

        if (exchange.getRefreshToken() != null) {
            result.put("refresh_token", exchange.getRefreshToken());
        }

        if (exchange.getSessionId() != null) {
            result.put("session_id", exchange.getSessionId());
        }

        log.info("Successfully exchanged OAuth code for user: {}", exchange.getUserId());
        return ResponseEntity.ok(result);
    }

    /**
     * Set exchange code cookie.
     */
    private void setExchangeCodeCookie(HttpServletResponse response, String exchangeCode) {
        Cookie cookie = new Cookie(EXCHANGE_CODE_COOKIE, exchangeCode);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Only send over HTTPS
        cookie.setPath("/api/auth/oauth/token");
        cookie.setMaxAge(EXCHANGE_CODE_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * Get exchange code from cookie.
     */
    private String getExchangeCodeFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (EXCHANGE_CODE_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Clear exchange code cookie.
     */
    private void clearExchangeCodeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(EXCHANGE_CODE_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/oauth/token");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
