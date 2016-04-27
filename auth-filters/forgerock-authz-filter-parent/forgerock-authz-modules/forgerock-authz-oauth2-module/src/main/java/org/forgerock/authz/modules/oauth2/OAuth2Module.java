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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private volatile Set<String> requiredScopes = new HashSet<>();
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
            final AuthorizationContext context) {

        if (accessToken != null) {
            // Verify is valid and not expired
            final Promise<AccessTokenValidationResponse, OAuth2Exception> validationResponse;
            if (cacheEnabled) {
                final AccessTokenValidationResponse entry = cache.get(accessToken);
                if (entry != null) {
                    validationResponse = newResultPromise(entry);
                } else {
                    validationResponse = validateAccessToken(accessToken);
                }
            } else {
                validationResponse = validateAccessToken(accessToken);
            }

            return validationResponse
                    .then(new Function<AccessTokenValidationResponse, AuthorizationResult, AuthorizationException>() {
                        @Override
                        public AuthorizationResult apply(AccessTokenValidationResponse validationResponse) {
                            if (!validationResponse.isTokenValid()) {
                                logger.debug("Access Token is invalid");
                                return AuthorizationResult.accessDenied("Access Token is invalid.");
                            }

                            // Verify scope is sufficient?...
                            final Set<String> tokenScopes = validationResponse.getTokenScopes();
                            if (!tokenScopes.containsAll(requiredScopes)) {
                                logger.debug("Access Token does not contain required scopes");
                                return AuthorizationResult.accessDenied(
                                        "Access Token does not contain required scopes.");
                            }

                            // Get profile information?...
                            final Map<String, Object> profileInfo = validationResponse.getProfileInformation();
                            context.setAttribute(OAUTH2_PROFILE_INFO_CONTEXT_KEY, profileInfo);

                            return AuthorizationResult.accessPermitted();
                        }
                    }, new Function<OAuth2Exception, AuthorizationResult, AuthorizationException>() {
                        @Override
                        public AuthorizationResult apply(OAuth2Exception e) {
                            logger.error("Failed to validate Access Token.", e);
                            throw new AuthorizationException("Failed to validate Access Token.", e);
                        }
                    });
        } else {
            return newResultPromise(AuthorizationResult.accessDenied("Access Token is null."));
        }
    }

    /**
     * Validates the access token and if the cache is enabled will store the result in the cache for subsequent
     * requests.
     *
     * @param accessToken The access token to validate.
     * @return An AccessTokenValidationResponse containing the result of the validation.
     * @throws OAuth2Exception If the access token could not be validated.
     */
    private Promise<AccessTokenValidationResponse, OAuth2Exception> validateAccessToken(final String accessToken) {
        return accessTokenValidator.validate(accessToken)
                .thenOnResult(new ResultHandler<AccessTokenValidationResponse>() {
                    @Override
                    public void handleResult(AccessTokenValidationResponse validationResponse) {
                        if (cacheEnabled) {
                            cache.add(accessToken, validationResponse);
                        }
                    }
                });
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
