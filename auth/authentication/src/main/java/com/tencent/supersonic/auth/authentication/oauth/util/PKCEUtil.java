package com.tencent.supersonic.auth.authentication.oauth.util;

import com.tencent.supersonic.auth.authentication.oauth.model.PKCEChallenge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for PKCE (Proof Key for Code Exchange) operations. Implements RFC 7636 for OAuth
 * 2.0 PKCE extension.
 */
public final class PKCEUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_VERIFIER_LENGTH = 43; // RFC 7636 recommends 43-128 characters

    private PKCEUtil() {
        // Utility class
    }

    /**
     * Generate a new PKCE challenge.
     *
     * @return PKCEChallenge containing code verifier, code challenge, and method
     */
    public static PKCEChallenge generate() {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return new PKCEChallenge(codeVerifier, codeChallenge, "S256");
    }

    /**
     * Generate a random code verifier. Uses URL-safe Base64 encoding as specified in RFC 7636.
     *
     * @return random code verifier string
     */
    public static String generateCodeVerifier() {
        return generateNonce();
    }

    /**
     * Generate a code challenge from a code verifier using SHA-256. Implements the S256 method as
     * specified in RFC 7636.
     *
     * @param codeVerifier the code verifier to hash
     * @return the code challenge string
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify that a code verifier matches a code challenge.
     *
     * @param codeVerifier the code verifier to verify
     * @param codeChallenge the expected code challenge
     * @return true if the code verifier produces the code challenge
     */
    public static boolean verify(String codeVerifier, String codeChallenge) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }
        String computedChallenge = generateCodeChallenge(codeVerifier);
        return codeChallenge.equals(computedChallenge);
    }

    /**
     * Generate a random state parameter for CSRF protection.
     *
     * @return random state string
     */
    public static String generateState() {
        return generateNonce();
    }

    /**
     * Generate a random nonce for OpenID Connect.
     *
     * @return random nonce string
     */
    public static String generateNonce() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
