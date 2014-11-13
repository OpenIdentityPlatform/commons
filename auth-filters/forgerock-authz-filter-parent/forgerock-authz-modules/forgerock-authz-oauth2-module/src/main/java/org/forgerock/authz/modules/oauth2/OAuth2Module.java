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

import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>OAuth2 authorization module that provide authorization based on an access token included in the request.</p>
 *
 * <p>The module uses AccessTokenValidators to perform the actual validation of access tokens and retrieve user profile
 * information as a method of providing a layer of abstraction so the module can work with many different OAuth2
 * Providers.</p>
 *
 * <p>Note: the individual AccessTokenValidators may required their own configuration.</p>
 *
 * @since 1.4.0
 */
public class OAuth2Module {

    private final Logger logger = LoggerFactory.getLogger(OAuth2Module.class);

    /**
     * Key for the Authorization Context for the profile information for the OAuth2 Access Token used to authorize
     * the request.
     */
    public static final String OAUTH2_PROFILE_INFO_CONTEXT_KEY = "org.forgerock.authorization.context.oauth2";

    private volatile OAuth2AccessTokenValidator accessTokenValidator;
    private volatile Set<String> requiredScopes = new HashSet<String>();
    private volatile boolean cacheEnabled = true;
    private volatile AccessTokenValidationCache cache;

    /**
     * Creates a new {@code OAuth2Module} instance with the provided configuration.
     *
     * @param accessTokenValidator A {@code OAuth2AccessTokenValidator} instance.
     * @param requiredScopes The required OAuth2 scopes for the request to be authorized.
     * @param cacheEnabled {@code true} if the cache should be used.
     * @param cacheSize The size of the cache. Only used if {@code cacheEnabled} is set to
     */
    public OAuth2Module(OAuth2AccessTokenValidator accessTokenValidator,
            Set<String> requiredScopes, boolean cacheEnabled, int cacheSize) {
        this.accessTokenValidator = accessTokenValidator;
        this.requiredScopes = requiredScopes;
        this.cacheEnabled = cacheEnabled;
        this.cache = new AccessTokenValidationCacheFactory().getCache(cacheSize);
    }

    /**
     * Creates a new {@code OAuth2Module} test instance.
     *
     * @param cacheFactory A {@code AccessTokenValidationCacheFactory} instance.
     * @param accessTokenValidator A {@code OAuth2AccessTokenValidator} instance.
     * @param requiredScopes The required OAuth2 scopes for the request to be authorized.
     * @param cacheEnabled {@code true} if the cache should be used.
     * @param cacheSize The size of the cache. Only used if {@code cacheEnabled} is set to
     */
    OAuth2Module(AccessTokenValidationCacheFactory cacheFactory, OAuth2AccessTokenValidator accessTokenValidator,
            Set<String> requiredScopes, boolean cacheEnabled, int cacheSize) {
        this.accessTokenValidator = accessTokenValidator;
        this.requiredScopes = requiredScopes;
        this.cacheEnabled = cacheEnabled;
        this.cache = cacheFactory.getCache(cacheSize);
    }

    /**
     * Determines whether a request is authorized to access the resource based on the validity of an access token
     * the scopes of the access token.
     *
     * @param accessToken {@inheritDoc}
     * @param context {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthorizationException {@inheritDoc}
     */
    public Promise<AuthorizationResult, AuthorizationException> authorize(String accessToken,
                                                                          AuthorizationContext context) {

        if (accessToken != null) {

            // Verify is valid and not expired
            final AccessTokenValidationResponse validationResponse;
            try {
                if (cacheEnabled) {
                    final AccessTokenValidationResponse entry = cache.get(accessToken);
                    if (entry != null) {
                        validationResponse = entry;
                    } else {
                        validationResponse = validateAccessToken(accessToken);
                    }
                } else {
                    validationResponse = validateAccessToken(accessToken);
                }

                if (!validationResponse.isTokenValid()) {
                    logger.debug("Access Token is invalid");
                    return Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("Access Token is invalid."));
                }

                // Verify scope is sufficient?...
                final Set<String> tokenScopes = validationResponse.getTokenScopes();
                if (!tokenScopes.containsAll(requiredScopes)) {
                    logger.debug("Access Token does not contain required scopes");
                    return Promises.newSuccessfulPromise(AuthorizationResult.accessDenied(
                            "Access Token does not contain required scopes."));
                }

                // Get profile information?...
                final Map<String, Object> profileInfo = validationResponse.getProfileInformation();
                context.setAttribute(OAUTH2_PROFILE_INFO_CONTEXT_KEY, profileInfo);

                return Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());
            } catch (OAuth2Exception e) {
                logger.error("Failed to validate Access Token.", e);
                return Promises.newFailedPromise(new AuthorizationException("Failed to validate Access Token.", e));
            }
        }

        return Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("Access Token is null."));
    }

    /**
     * Validates the access token and if the cache is enabled will store the result in the cache for subsequent
     * requests.
     *
     * @param accessToken The access token to validate.
     * @return An AccessTokenValidationResponse containing the result of the validation.
     * @throws OAuth2Exception If the access token could not be validated.
     */
    private AccessTokenValidationResponse validateAccessToken(final String accessToken) {
        final AccessTokenValidationResponse validationResponse = accessTokenValidator.validate(accessToken);
        if (cacheEnabled) {
            cache.add(accessToken, validationResponse);
        }
        return validationResponse;
    }

    /**
     * Factory class for creating new instances of the {@link AccessTokenValidationCache}.
     *
     * @since 1.4.0
     */
    static class AccessTokenValidationCacheFactory {

        /**
         * Creates a new {@code AccessTokenValidationCache} with the given size.
         *
         * @param size The size of the cache.
         * @return A new {@code AccessTokenValidationCache} instance.
         */
        AccessTokenValidationCache getCache(final int size) {
            return new AccessTokenValidationCache(size);
        }
    }
}
