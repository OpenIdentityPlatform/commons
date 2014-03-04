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

import org.forgerock.authz.AuthorizationContext;
import org.forgerock.authz.AuthorizationException;
import org.forgerock.authz.AuthorizationModule;
import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 authorization module that provide authorization based on an access token included in the request.
 * <br/>
 * The module uses AccessTokenValidators to perform the actual validation of access tokens and retrieve user profile
 * information as a method of providing a layer of abstraction so the module can work with many different OAuth2
 * Providers.
 * <br/>
 * This module requires the configuration given at initialisation to contain the following entries:
 * <ul>
 *     <li>access-token-validator-class - the fully qualified class name of the AccessTokenValidator class which will
 *     provide the validation logic for determining if an access token is valid or not</li>
 *     <li>required-scopes - the scopes the access token must have to be authorized. Defaults to an empty list</li>
 *     <li>cacheEnabled - whether to cache response. Defaults to true</li>
 *     <li>cache-size - the maximum size of the cache. Defaults to 100</li>
 * </ul>
 * <br/>
 * Note: the individual AccessTokenValidators may required their own configuration.
 *
 * @since 1.4.0
 */
public class OAuth2Module implements AuthorizationModule {

    private final Logger logger = LoggerFactory.getLogger(OAuth2Module.class);

    /**
     * Key for the Authorization Context for the profile information for the OAuth2 Access Token used to authorize
     * the request.
     */
    public static final String OAUTH2_PROFILE_INFO_CONTEXT_KEY = "OAUTH2_PROFILE_INFO";

    private final AccessTokenValidatorFactory validatorFactory;
    private final AccessTokenValidationCacheFactory cacheFactory;

    private volatile OAuth2AccessTokenValidator accessTokenValidator;
    private volatile Set<String> requiredScopes = new HashSet<String>();
    private volatile boolean cacheEnabled = true;
    private volatile AccessTokenValidationCache cache;

    /**
     * Constructs a new OAuth2Module.
     */
    public OAuth2Module() {
        this(new AccessTokenValidatorFactory(), new AccessTokenValidationCacheFactory());
    }

    /**
     * Constructs a new OAuth2Module, for use in tests.
     *
     * @param validatorFactory An instance of the AccessTokenValidatorFactory.
     * @param cacheFactory An instance of the AccessTokenValidationCacheFactory.
     */
    OAuth2Module(final AccessTokenValidatorFactory validatorFactory,
            final AccessTokenValidationCacheFactory cacheFactory) {
        this.validatorFactory = validatorFactory;
        this.cacheFactory = cacheFactory;
    }

    /**
     * Initialises the OAuth2 Module with the AccessTokenValidator class and the required scopes to grant authorization
     * for the request.
     *
     * @param config {@inheritDoc}
     * @throws AuthorizationException {@inheritDoc}
     */
    @Override
    public void initialise(final JsonValue config) {

        final String accessTokenValidatorClassName = config.get("access-token-validator-class").required().asString();

        accessTokenValidator = validatorFactory.getInstance(accessTokenValidatorClassName, config);

        requiredScopes.addAll(config.get("required-scopes").defaultTo(Collections.<String>emptyList())
                .asList(String.class));

        cacheEnabled = config.get("cache-enabled").defaultTo(true).asBoolean();

        final int cacheSize = config.get("cache-size").defaultTo(100).asInteger();
        cache = cacheFactory.getCache(cacheSize);
    }

    /**
     * Determines whether a request is authorized to access the resource based on the validity of an access token
     * the scopes of the access token.
     *
     * @param req {@inheritDoc}
     * @param context {@inheritDoc}
     * @return {@inheritDoc
     * @throws AuthorizationException {@inheritDoc}
     */
    @Override
    public boolean authorize(final HttpServletRequest req, final AuthorizationContext context) {

        final String accessToken = getAccessToken(req);
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
                    return false;
                }

                // Verify scope is sufficient?...
                final Set<String> tokenScopes = validationResponse.getTokenScopes();
                if (!tokenScopes.containsAll(requiredScopes)) {
                    logger.debug("Access Token does not contain required scopes");
                    return false;
                }

                // Get profile information?...
                final Map<String, Object> profileInfo = validationResponse.getProfileInformation();
                context.setAttribute(OAUTH2_PROFILE_INFO_CONTEXT_KEY, profileInfo);

                return true;
            } catch (OAuth2Exception e) {
                logger.error("Failed to validate Access Token.", e);
                throw new AuthorizationException("Failed to validate Access Token.", e);
            }
        }

        return false;
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
     * Pulls the access token of off the request, by looking for the Authorization header containing a Bearer token.
     *
     * @param req The HttpServletRequest.
     * @return The access token, or <code>null</code> if the access token was not present or was not using Bearer
     * authorization.
     */
    private String getAccessToken(final HttpServletRequest req) {

        final String header = req.getHeader("Authorization");
        if (header == null) {
            logger.debug("Authorization header not present");
            return null;
        }
        final int index = header.indexOf(' ');
        if (index <= 0) {
            logger.debug("Invalid Authorization header");
            return null;
        }

        final String tokenType = header.substring(0, index);

        if ("BEARER".equalsIgnoreCase(tokenType)) {
            return header.substring(index + 1);
        }

        logger.debug("Unknown Access Token Type, {}", tokenType);
        return null;
    }

    /**
     * No state to destroy.
     */
    @Override
    public void destroy() {

    }

    static class AccessTokenValidationCacheFactory {

        AccessTokenValidationCache getCache(final int size) {
            return new AccessTokenValidationCache(size);
        }
    }
}
