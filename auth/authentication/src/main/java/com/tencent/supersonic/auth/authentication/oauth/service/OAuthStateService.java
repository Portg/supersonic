package com.tencent.supersonic.auth.authentication.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.authentication.oauth.model.PKCEChallenge;
import com.tencent.supersonic.auth.authentication.oauth.provider.OAuthException;
import com.tencent.supersonic.auth.authentication.oauth.util.PKCEUtil;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthStateDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.OAuthStateDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service for managing OAuth state parameters and PKCE challenges.
 */
@Slf4j
@Service
public class OAuthStateService {

    private final OAuthStateDOMapper stateMapper;
    private final OAuthConfig oauthConfig;

    public OAuthStateService(OAuthStateDOMapper stateMapper, OAuthConfig oauthConfig) {
        this.stateMapper = stateMapper;
        this.oauthConfig = oauthConfig;
    }

    /**
     * Create a new OAuth state with PKCE challenge.
     */
    @Transactional
    public OAuthStateDO createState(String providerName, String redirectUri, boolean pkceEnabled) {
        String state = PKCEUtil.generateState();
        String nonce = PKCEUtil.generateNonce();
        PKCEChallenge pkce = pkceEnabled ? PKCEUtil.generate() : null;

        OAuthStateDO stateDO = new OAuthStateDO();
        stateDO.setState(state);
        stateDO.setProviderName(providerName);
        stateDO.setRedirectUri(redirectUri);
        stateDO.setNonce(nonce);
        stateDO.setCreatedAt(new Date());
        stateDO.setExpiresAt(new Date(System.currentTimeMillis() + oauthConfig.getStateTimeout()));
        stateDO.setUsed(0);

        if (pkce != null) {
            stateDO.setCodeVerifier(pkce.getCodeVerifier());
        }

        stateMapper.insert(stateDO);
        log.debug("Created OAuth state: {} for provider: {}", state, providerName);

        return stateDO;
    }

    /**
     * Validate and consume a state parameter.
     */
    @Transactional
    public OAuthStateDO validateAndConsumeState(String state) throws OAuthException {
        if (state == null || state.isEmpty()) {
            throw OAuthException.invalidState();
        }

        LambdaQueryWrapper<OAuthStateDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthStateDO::getState, state);

        OAuthStateDO stateDO = stateMapper.selectOne(query);

        if (stateDO == null) {
            log.warn("OAuth state not found: {}", state);
            throw OAuthException.invalidState();
        }

        if (stateDO.getUsed() != null && stateDO.getUsed() == 1) {
            log.warn("OAuth state already used: {}", state);
            throw OAuthException.invalidState();
        }

        if (stateDO.getExpiresAt() != null && stateDO.getExpiresAt().before(new Date())) {
            log.warn("OAuth state expired: {}", state);
            stateMapper.deleteById(stateDO.getId());
            throw OAuthException.invalidState();
        }

        // Mark as used
        stateDO.setUsed(1);
        stateMapper.updateById(stateDO);

        log.debug("Validated and consumed OAuth state: {}", state);
        return stateDO;
    }

    /**
     * Get state information without consuming it (for validation purposes).
     */
    public OAuthStateDO getState(String state) {
        if (state == null || state.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<OAuthStateDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthStateDO::getState, state);

        return stateMapper.selectOne(query);
    }

    /**
     * Delete a state.
     */
    @Transactional
    public void deleteState(String state) {
        if (state == null || state.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<OAuthStateDO> query = new LambdaQueryWrapper<>();
        query.eq(OAuthStateDO::getState, state);

        stateMapper.delete(query);
    }

    /**
     * Clean up expired states (scheduled task).
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Every 5 minutes, delay 1 minute on
                                                         // startup
    @Transactional
    public void cleanupExpiredStates() {
        try {
            LambdaQueryWrapper<OAuthStateDO> query = new LambdaQueryWrapper<>();
            query.lt(OAuthStateDO::getExpiresAt, new Date());

            int deleted = stateMapper.delete(query);
            if (deleted > 0) {
                log.info("Cleaned up {} expired OAuth states", deleted);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup expired OAuth states: {}", e.getMessage());
        }
    }
}
