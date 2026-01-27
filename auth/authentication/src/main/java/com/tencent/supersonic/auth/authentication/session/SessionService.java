package com.tencent.supersonic.auth.authentication.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.config.SessionConfig;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserSessionDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserSessionDOMapper;
import com.tencent.supersonic.auth.authentication.refresh.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Service for managing user sessions.
 */
@Slf4j
@Service
public class SessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserSessionDOMapper sessionMapper;
    private final SessionConfig sessionConfig;
    private final RefreshTokenService refreshTokenService;

    public SessionService(UserSessionDOMapper sessionMapper, SessionConfig sessionConfig,
            RefreshTokenService refreshTokenService) {
        this.sessionMapper = sessionMapper;
        this.sessionConfig = sessionConfig;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Check if session management is enabled.
     */
    public boolean isEnabled() {
        return sessionConfig.isEnabled();
    }

    /**
     * Create a new session.
     */
    @Transactional
    public UserSessionDO createSession(Long userId, String authMethod, String providerName,
            String ipAddress, String userAgent) {
        if (!isEnabled()) {
            return null;
        }

        // Check max concurrent sessions
        if (sessionConfig.exceedsMaxConcurrent(getActiveSessionCount(userId))) {
            // Revoke oldest session
            revokeOldestSession(userId);
        }

        // Generate session ID
        byte[] sessionIdBytes = new byte[32];
        SECURE_RANDOM.nextBytes(sessionIdBytes);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(sessionIdBytes);

        UserSessionDO session = new UserSessionDO();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setAuthMethod(authMethod);
        session.setProviderName(providerName);
        session.setCreatedAt(new Date());
        session.setLastActivityAt(new Date());
        session.setExpiresAt(new Date(System.currentTimeMillis() + sessionConfig.getTimeout()));
        session.setIpAddress(ipAddress);
        session.setUserAgent(truncateUserAgent(userAgent));
        session.setRevoked(0);

        sessionMapper.insert(session);
        log.debug("Created session {} for user {}", sessionId, userId);

        return session;
    }

    /**
     * Get a session by session ID.
     */
    public UserSessionDO getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<UserSessionDO> query = new LambdaQueryWrapper<>();
        query.eq(UserSessionDO::getSessionId, sessionId);

        return sessionMapper.selectOne(query);
    }

    /**
     * Validate a session.
     */
    public UserSessionDO validateSession(String sessionId) {
        UserSessionDO session = getSession(sessionId);
        if (session == null || !session.isValid()) {
            return null;
        }
        return session;
    }

    /**
     * Update session last activity time.
     */
    @Transactional
    public void updateLastActivity(String sessionId) {
        if (!isEnabled() || sessionId == null) {
            return;
        }

        UserSessionDO session = getSession(sessionId);
        if (session != null && session.isValid()) {
            session.setLastActivityAt(new Date());
            sessionMapper.updateById(session);
        }
    }

    /**
     * Revoke a specific session.
     */
    @Transactional
    public boolean revokeSession(String sessionId, String reason) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        UserSessionDO session = getSession(sessionId);
        if (session == null || session.isRevoked()) {
            return false;
        }

        session.setRevoked(1);
        session.setRevokedAt(new Date());
        session.setRevokedReason(reason);
        sessionMapper.updateById(session);

        // Revoke associated refresh tokens
        refreshTokenService.revokeTokensForSession(session.getId());

        log.info("Revoked session {} for user {}: {}", sessionId, session.getUserId(), reason);
        return true;
    }

    /**
     * Revoke all sessions for a user.
     */
    @Transactional
    public int revokeAllSessionsForUser(Long userId, String reason) {
        return revokeAllSessionsForUser(userId, reason, null);
    }

    /**
     * Revoke all sessions for a user except the specified session.
     */
    @Transactional
    public int revokeAllSessionsForUser(Long userId, String reason, String exceptSessionId) {
        LambdaQueryWrapper<UserSessionDO> query = new LambdaQueryWrapper<>();
        query.eq(UserSessionDO::getUserId, userId).eq(UserSessionDO::getRevoked, 0);

        if (exceptSessionId != null) {
            query.ne(UserSessionDO::getSessionId, exceptSessionId);
        }

        List<UserSessionDO> sessions = sessionMapper.selectList(query);

        int revoked = 0;
        for (UserSessionDO session : sessions) {
            session.setRevoked(1);
            session.setRevokedAt(new Date());
            session.setRevokedReason(reason);
            sessionMapper.updateById(session);

            // Revoke associated refresh tokens
            refreshTokenService.revokeTokensForSession(session.getId());

            revoked++;
        }

        log.info("Revoked {} sessions for user {}: {}", revoked, userId, reason);
        return revoked;
    }

    /**
     * Get all active sessions for a user.
     */
    public List<UserSessionDO> getActiveSessionsForUser(Long userId) {
        LambdaQueryWrapper<UserSessionDO> query = new LambdaQueryWrapper<>();
        query.eq(UserSessionDO::getUserId, userId).eq(UserSessionDO::getRevoked, 0)
                .gt(UserSessionDO::getExpiresAt, new Date())
                .orderByDesc(UserSessionDO::getLastActivityAt);

        return sessionMapper.selectList(query);
    }

    /**
     * Get the count of active sessions for a user.
     */
    public int getActiveSessionCount(Long userId) {
        LambdaQueryWrapper<UserSessionDO> query = new LambdaQueryWrapper<>();
        query.eq(UserSessionDO::getUserId, userId).eq(UserSessionDO::getRevoked, 0)
                .gt(UserSessionDO::getExpiresAt, new Date());

        return Math.toIntExact(sessionMapper.selectCount(query));
    }

    /**
     * Revoke the oldest session for a user.
     */
    private void revokeOldestSession(Long userId) {
        LambdaQueryWrapper<UserSessionDO> query = new LambdaQueryWrapper<>();
        query.eq(UserSessionDO::getUserId, userId).eq(UserSessionDO::getRevoked, 0)
                .gt(UserSessionDO::getExpiresAt, new Date()).orderByAsc(UserSessionDO::getCreatedAt)
                .last("LIMIT 1");

        UserSessionDO oldest = sessionMapper.selectOne(query);
        if (oldest != null) {
            revokeSession(oldest.getSessionId(), "Max concurrent sessions exceeded");
        }
    }

    /**
     * Truncate user agent string if too long.
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.length() > 500) {
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }

    /**
     * Clean up expired sessions (scheduled task).
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000) // Every hour, delay 1 minute on startup
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            // Delete expired sessions
            LambdaQueryWrapper<UserSessionDO> expiredQuery = new LambdaQueryWrapper<>();
            expiredQuery.lt(UserSessionDO::getExpiresAt, new Date());

            int expiredDeleted = sessionMapper.delete(expiredQuery);

            // Delete revoked sessions older than 7 days
            LambdaQueryWrapper<UserSessionDO> revokedQuery = new LambdaQueryWrapper<>();
            revokedQuery.eq(UserSessionDO::getRevoked, 1).lt(UserSessionDO::getRevokedAt,
                    new Date(System.currentTimeMillis() - 7 * 86400000L));

            int revokedDeleted = sessionMapper.delete(revokedQuery);

            if (expiredDeleted > 0 || revokedDeleted > 0) {
                log.info("Cleaned up sessions: {} expired, {} old revoked", expiredDeleted,
                        revokedDeleted);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup expired sessions: {}", e.getMessage());
        }
    }
}
