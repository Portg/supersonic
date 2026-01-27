package com.tencent.supersonic.auth.authentication.oauth.provider;

/**
 * Exception thrown during OAuth operations.
 */
public class OAuthException extends Exception {

    private final String errorCode;
    private final String errorDescription;

    public OAuthException(String message) {
        super(message);
        this.errorCode = "oauth_error";
        this.errorDescription = message;
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "oauth_error";
        this.errorDescription = message;
    }

    public OAuthException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public OAuthException(String errorCode, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Create an exception for invalid state.
     */
    public static OAuthException invalidState() {
        return new OAuthException("invalid_state", "The state parameter is invalid or has expired");
    }

    /**
     * Create an exception for invalid token.
     */
    public static OAuthException invalidToken() {
        return new OAuthException("invalid_token", "The token is invalid or has expired");
    }

    /**
     * Create an exception for provider not configured.
     */
    public static OAuthException providerNotConfigured(String providerName) {
        return new OAuthException("provider_not_configured",
                "OAuth provider '" + providerName + "' is not configured");
    }

    /**
     * Create an exception for token exchange failure.
     */
    public static OAuthException tokenExchangeFailed(String reason) {
        return new OAuthException("token_exchange_failed",
                "Failed to exchange code for tokens: " + reason);
    }

    /**
     * Create an exception for user info fetch failure.
     */
    public static OAuthException userInfoFetchFailed(String reason) {
        return new OAuthException("user_info_failed", "Failed to fetch user info: " + reason);
    }
}
