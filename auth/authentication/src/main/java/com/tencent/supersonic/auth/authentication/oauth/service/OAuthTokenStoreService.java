package com.tencent.supersonic.auth.authentication.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthTokenResponse;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.OAuthTokenDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Service for storing and retrieving OAuth tokens.
 */
@Slf4j
@Service
public class OAuthTokenStoreService {

    private final OAuthTokenDOMapper tokenMapper;

    public OAuthTokenStoreService(OAuthTokenDOMapper tokenMapper) {
        this.tokenMapper = tokenMapper;
    }

    /**
     * Store OAuth tokens for a user.
     */
    @Transactional
    public OAuthTokenDO storeTokens(Long userId, String providerName,
            OAuthTokenResponse tokenResponse) {
        // Check if tokens already exist for this user and provider
        OAuthTokenDO existing = getTokens(userId, providerName);

        OAuthTokenDO tokenDO;
        if (existing != null) {
            tokenDO = existing;
        } else {
            tokenDO = new OAuthTokenDO();
            tokenDO.setUserId(userId);
            tokenDO.setProviderName(providerName);
            tokenDO.setCreatedAt(new Date());
        }

        tokenDO.setAccessToken(tokenResponse.getAccessToken());
        tokenDO.setRefreshToken(tokenResponse.getRefreshToken());
        tokenDO.setIdToken(tokenResponse.getIdToken());
        tokenDO.setTokenType(tokenResponse.getTokenType());
        tokenDO.setScopes(tokenResponse.getScope());
        tokenDO.setUpdatedAt(new Date());

        if (tokenResponse.getExpiresIn() != null) {
            tokenDO.setExpiresAt(
                    new Date(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000));
        }

        if (tokenResponse.getRefreshExpiresIn() != null) {
            tokenDO.setRefreshExpiresAt(new Date(
                    System.currentTimeMillis() + tokenResponse.getRefreshExpiresIn() * 1000));
        }

        if (existing != null) {
            tokenMapper.updateById(tokenDO);
            log.debug("Updated OAuth tokens for user {} provider {}", userId, providerName);
        } else {
            tokenMapper.insert(tokenDO);
            log.debug("Stored new OAuth tokens for user {} provider {}", userId, providerName);
        }

        return tokenDO;
    }

    /**
     * Get OAuth tokens for a user and provider.
     */
    public OAuthTokenDO getTokens(Long userId, String providerName) {
        LambdaQueryWrapper<OAuthTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthTokenDO::getUserId, userId).eq(OAuthTokenDO::getProviderName, providerName);

        return tokenMapper.selectOne(query);
    }

    /**
     * Get all OAuth tokens for a user.
     */
    public List<OAuthTokenDO> getTokensForUser(Long userId) {
        LambdaQueryWrapper<OAuthTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthTokenDO::getUserId, userId);

        return tokenMapper.selectList(query);
    }

    /**
     * Delete OAuth tokens for a user and provider.
     */
    @Transactional
    public void deleteTokens(Long userId, String providerName) {
        LambdaQueryWrapper<OAuthTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthTokenDO::getUserId, userId).eq(OAuthTokenDO::getProviderName, providerName);

        tokenMapper.delete(query);
        log.debug("Deleted OAuth tokens for user {} provider {}", userId, providerName);
    }

    /**
     * Delete all OAuth tokens for a user.
     */
    @Transactional
    public void deleteAllTokensForUser(Long userId) {
        LambdaQueryWrapper<OAuthTokenDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthTokenDO::getUserId, userId);

        int deleted = tokenMapper.delete(query);
        log.debug("Deleted {} OAuth tokens for user {}", deleted, userId);
    }

    /**
     * Check if the access token is expired.
     */
    public boolean isAccessTokenExpired(OAuthTokenDO tokenDO) {
        if (tokenDO == null || tokenDO.getExpiresAt() == null) {
            return true;
        }
        // Consider expired if within 60 seconds of expiration
        return tokenDO.getExpiresAt().before(new Date(System.currentTimeMillis() + 60000));
    }

    /**
     * Check if there is a valid refresh token available.
     */
    public boolean hasValidRefreshToken(OAuthTokenDO tokenDO) {
        if (tokenDO == null || tokenDO.getRefreshToken() == null
                || tokenDO.getRefreshToken().isEmpty()) {
            return false;
        }
        if (tokenDO.getRefreshExpiresAt() != null) {
            return tokenDO.getRefreshExpiresAt().after(new Date());
        }
        return true; // If no expiry set, assume it's valid
    }
}
