package com.tencent.supersonic.auth.authentication.oauth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PKCE (Proof Key for Code Exchange) challenge for OAuth 2.0.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PKCEChallenge {

    /**
     * The code verifier (random string to be sent with token exchange)
     */
    private String codeVerifier;

    /**
     * The code challenge (derived from code verifier, sent with authorization request)
     */
    private String codeChallenge;

    /**
     * The code challenge method (usually "S256")
     */
    private String codeChallengeMethod;
}
