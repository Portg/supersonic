package com.tencent.supersonic.auth.authentication.oauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * User information retrieved from OAuth provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {

    /**
     * Subject identifier (unique user ID from provider)
     */
    private String sub;

    /**
     * User's full name
     */
    private String name;

    /**
     * User's given name (first name)
     */
    private String givenName;

    /**
     * User's family name (last name)
     */
    private String familyName;

    /**
     * User's email address
     */
    private String email;

    /**
     * Whether the email is verified
     */
    private Boolean emailVerified;

    /**
     * User's profile picture URL
     */
    private String picture;

    /**
     * User's locale
     */
    private String locale;

    /**
     * OAuth provider name
     */
    private String provider;

    /**
     * Additional claims from the ID token or user info response
     */
    @Builder.Default
    private Map<String, Object> additionalClaims = new HashMap<>();

    /**
     * Get the display name, falling back to email if name is not available.
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (givenName != null && !givenName.isEmpty()) {
            if (familyName != null && !familyName.isEmpty()) {
                return givenName + " " + familyName;
            }
            return givenName;
        }
        return email;
    }

    /**
     * Get the username, preferring email.
     */
    public String getUsername() {
        if (email != null && !email.isEmpty()) {
            return email;
        }
        return sub;
    }
}
