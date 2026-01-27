package com.tencent.supersonic.auth.authentication.refresh;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.config.RefreshTokenConfig;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RefreshTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.RefreshTokenDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Service for managing refresh tokens.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenDOMapper refreshTokenMapper;
    private final RefreshTokenConfig refreshTokenConfig;

    public RefreshTokenService(RefreshTokenDOMapper refreshTokenMapper,
            RefreshTokenConfig refreshTokenConfig) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenConfig = refreshTokenConfig;
    }

    /**
     * Check if refresh tokens are enabled.
     */
    public boolean isEnabled() {
        return refreshTokenConfig.isEnabled();
    }

    /**
     * Generate a new refresh token for a user.
     */
    @Transactional
    public RefreshTokenResult generateRefreshToken(Long userId, Long sessionId, String deviceInfo,
            String ipAddress) {
        if (!isEnabled()) {
            return null;
        }

        // Check max tokens per user
        if (refreshTokenConfig.getMaxTokensPerUser() > 0) {
            cleanupExcessTokens(userId);
        }

        // Generate random token
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash token for storage
        String tokenHash = hashToken(rawToken);

        RefreshTokenDO tokenDO = new RefreshTokenDO();
        tokenDO.setTokenHash(tokenHash);
        tokenDO.setUserId(userId);
        tokenDO.setSessionId(sessionId);
        tokenDO.setIssuedAt(new Date());
        tokenDO.setExpiresAt(
                new Date(System.currentTimeMillis() + refreshTokenConfig.getTimeout()));
        tokenDO.setRevoked(0);
        tokenDO.setDeviceInfo(deviceInfo);
        tokenDO.setIpAddress(ipAddress);

        refreshTokenMapper.insert(tokenDO);
        log.debug("Generated refresh token for user {}", userId);

        return new RefreshTokenResult(rawToken, tokenDO);
    }

    /**
     * Validate a refresh token and return associated data if valid.
     */
    public RefreshTokenDO validateRefreshToken(String rawToken) {
        if (!isEnabled() || rawToken == null || rawToken.isEmpty()) {
            return null;
        }

        String tokenHash = hashToken(rawToken);

        LambdaQueryWrapper<RefreshTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(RefreshTokenDO::getTokenHash, tokenHash);

        RefreshTokenDO tokenDO = refreshTokenMapper.selectOne(query);

        if (tokenDO == null) {
            log.debug("Refresh token not found");
            return null;
        }

        if (!tokenDO.isValid()) {
            log.debug("Refresh token is invalid (revoked or expired)");
            return null;
        }

        return tokenDO;
    }

    /**
     * Refresh an access token using a refresh token. If rotation is enabled, the old refresh token
     * is revoked and a new one is issued.
     */
    @Transactional
    public RefreshResult refresh(String rawRefreshToken, String deviceInfo, String ipAddress) {
        RefreshTokenDO tokenDO = validateRefreshToken(rawRefreshToken);
        if (tokenDO == null) {
            return null;
        }

        RefreshTokenResult newRefreshToken = null;

        if (refreshTokenConfig.isRotation()) {
            // Revoke old token
            tokenDO.setRevoked(1);
            tokenDO.setRevokedAt(new Date());
            refreshTokenMapper.updateById(tokenDO);

            // Generate new refresh token
            newRefreshToken = generateRefreshToken(tokenDO.getUserId(), tokenDO.getSessionId(),
                    deviceInfo != null ? deviceInfo : tokenDO.getDeviceInfo(),
                    ipAddress != null ? ipAddress : tokenDO.getIpAddress());
        }

        return new RefreshResult(tokenDO.getUserId(), tokenDO.getSessionId(), newRefreshToken);
    }

    /**
     * Revoke a specific refresh token.
     */
    @Transactional
    public boolean revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return false;
        }

        String tokenHash = hashToken(rawToken);

        LambdaQueryWrapper<RefreshTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(RefreshTokenDO::getTokenHash, tokenHash);

        RefreshTokenDO tokenDO = refreshTokenMapper.selectOne(query);
        if (tokenDO == null || tokenDO.isRevoked()) {
            return false;
        }

        tokenDO.setRevoked(1);
        tokenDO.setRevokedAt(new Date());
        refreshTokenMapper.updateById(tokenDO);

        log.debug("Revoked refresh token for user {}", tokenDO.getUserId());
        return true;
    }

    /**
     * Revoke all refresh tokens for a user.
     */
    @Transactional
    public int revokeAllTokensForUser(Long userId) {
        LambdaQueryWrapper<RefreshTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(RefreshTokenDO::getUserId, userId).eq(RefreshTokenDO::getRevoked, 0);

        List<RefreshTokenDO> tokens = refreshTokenMapper.selectList(query);

        int revoked = 0;
        for (RefreshTokenDO token : tokens) {
            token.setRevoked(1);
            token.setRevokedAt(new Date());
            refreshTokenMapper.updateById(token);
            revoked++;
        }

        log.info("Revoked {} refresh tokens for user {}", revoked, userId);
        return revoked;
    }

    /**
     * Revoke all refresh tokens for a session.
     */
    @Transactional
    public int revokeTokensForSession(Long sessionId) {
        if (sessionId == null) {
            return 0;
        }

        LambdaQueryWrapper<RefreshTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(RefreshTokenDO::getSessionId, sessionId).eq(RefreshTokenDO::getRevoked, 0);

        List<RefreshTokenDO> tokens = refreshTokenMapper.selectList(query);

        int revoked = 0;
        for (RefreshTokenDO token : tokens) {
            token.setRevoked(1);
            token.setRevokedAt(new Date());
            refreshTokenMapper.updateById(token);
            revoked++;
        }

        log.debug("Revoked {} refresh tokens for session {}", revoked, sessionId);
        return revoked;
    }

    /**
     * Get all active refresh tokens for a user.
     */
    public List<RefreshTokenDO> getActiveTokensForUser(Long userId) {
        LambdaQueryWrapper<RefreshTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(RefreshTokenDO::getUserId, userId).eq(RefreshTokenDO::getRevoked, 0)
                .gt(RefreshTokenDO::getExpiresAt, new Date())
                .orderByDesc(RefreshTokenDO::getIssuedAt);

        return refreshTokenMapper.selectList(query);
    }

    /**
     * Clean up excess tokens when max tokens per user is reached.
     */
    private void cleanupExcessTokens(Long userId) {
        List<RefreshTokenDO> activeTokens = getActiveTokensForUser(userId);

        if (activeTokens.size() >= refreshTokenConfig.getMaxTokensPerUser()) {
            // Revoke oldest tokens
            int toRevoke = activeTokens.size() - refreshTokenConfig.getMaxTokensPerUser() + 1;
            for (int i = activeTokens.size() - 1; i >= activeTokens.size() - toRevoke
                    && i >= 0; i--) {
                RefreshTokenDO token = activeTokens.get(i);
                token.setRevoked(1);
                token.setRevokedAt(new Date());
                refreshTokenMapper.updateById(token);
            }
            log.debug("Cleaned up {} excess refresh tokens for user {}", toRevoke, userId);
        }
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Clean up expired and old revoked tokens (scheduled task).
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000) // Every hour, delay 1 minute on startup
    @Transactional
    public void cleanupTokens() {
        try {
            // Delete expired tokens
            LambdaQueryWrapper<RefreshTokenDO> expiredQuery = new LambdaQueryWrapper<>();
            expiredQuery.lt(RefreshTokenDO::getExpiresAt, new Date());

            int expiredDeleted = refreshTokenMapper.delete(expiredQuery);

            // Delete revoked tokens older than 24 hours
            LambdaQueryWrapper<RefreshTokenDO> revokedQuery = new LambdaQueryWrapper<>();
            revokedQuery.eq(RefreshTokenDO::getRevoked, 1).lt(RefreshTokenDO::getRevokedAt,
                    new Date(System.currentTimeMillis() - 86400000));

            int revokedDeleted = refreshTokenMapper.delete(revokedQuery);

            if (expiredDeleted > 0 || revokedDeleted > 0) {
                log.info("Cleaned up refresh tokens: {} expired, {} old revoked", expiredDeleted,
                        revokedDeleted);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup refresh tokens: {}", e.getMessage());
        }
    }

    /**
     * Result of generating a refresh token.
     */
    public static class RefreshTokenResult {
        private final String rawToken;
        private final RefreshTokenDO tokenDO;

        public RefreshTokenResult(String rawToken, RefreshTokenDO tokenDO) {
            this.rawToken = rawToken;
            this.tokenDO = tokenDO;
        }

        public String getRawToken() {
            return rawToken;
        }

        public RefreshTokenDO getTokenDO() {
            return tokenDO;
        }
    }

    /**
     * Result of refreshing a token.
     */
    public static class RefreshResult {
        private final Long userId;
        private final Long sessionId;
        private final RefreshTokenResult newRefreshToken;

        public RefreshResult(Long userId, Long sessionId, RefreshTokenResult newRefreshToken) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.newRefreshToken = newRefreshToken;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public RefreshTokenResult getNewRefreshToken() {
            return newRefreshToken;
        }
    }
}
