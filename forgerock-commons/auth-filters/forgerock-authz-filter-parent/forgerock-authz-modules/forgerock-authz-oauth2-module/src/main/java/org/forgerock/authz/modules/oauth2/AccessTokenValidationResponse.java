/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Models the result of an access token validation.
 *
 * @since 1.4.0
 */
public class AccessTokenValidationResponse {

    private final long expiryTime;
    private final Map<String, Object> profileInfo;
    private final Set<String> scope;

    /**
     * Constructs a new AccessTokenValidationResponse.
     *
     * @param expiryTime The time at which the access token becomes invalid.
     * @param profileInfo The user profile information for the user who authorized the access token.
     * @param scope The scope of the access token.
     */
    public AccessTokenValidationResponse(long expiryTime, Map<String, Object> profileInfo, Set<String> scope) {
        this.expiryTime = expiryTime;
        this.profileInfo = Collections.unmodifiableMap(profileInfo);
        this.scope = Collections.unmodifiableSet(scope);
    }

    /**
     * Constructs a new AccessTokenValidationResponse, without any user profile information.
     *
     * @param expiryTime The time at which the access token becomes invalid.
     * @param scope The scope of the access token.
     */
    public AccessTokenValidationResponse(long expiryTime, Set<String> scope) {
        this(expiryTime, Collections.<String, Object>emptyMap(), scope);
    }

    /**
     * Constructs a new AccessTokenValidationResponse, without any user profile information or scope.
     *
     * @param expiryTime The time at which the access token becomes invalid.
     */
    public AccessTokenValidationResponse(long expiryTime) {
        this(expiryTime, Collections.<String, Object>emptyMap(), Collections.<String>emptySet());
    }

    /**
     * Returns whether the access token is valid.
     *
     * @return <code>true</code> if the access token is valid.
     */
    public boolean isTokenValid() {
        return expiryTime >= System.currentTimeMillis();
    }

    /**
     * Returns the expiry time of the access token.
     *
     * @return The access token expiry time.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Returns the users profile information.
     *
     * @return The users profile information.
     */
    public Map<String, Object> getProfileInformation() {
        return profileInfo;
    }

    /**
     * Returns the scope associated with the access token.
     *
     * @return The access token scope.
     */
    public Set<String> getTokenScopes() {
        return scope;
    }
}
