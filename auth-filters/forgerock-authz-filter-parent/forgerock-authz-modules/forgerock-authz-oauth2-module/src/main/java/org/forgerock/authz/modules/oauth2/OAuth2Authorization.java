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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Set;

import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.HttpClientHandler;

/**
 * Utility class providing convenience methods for creating {@link OAuth2CrestAuthorizationModule}s.
 *
 * @since 1.5.0
 */
public final class OAuth2Authorization {

    static final String TOKEN_INFO_ENDPOINT_KEY = "token-info-endpoint";
    static final String USER_INFO_ENDPOINT_KEY = "user-info-endpoint";

    /**
     * Private utility constructor.
     */
    private OAuth2Authorization() {
        throw new UnsupportedOperationException("OAuth2Authorization cannot be instantiated.");
    }

    /**
     * Creates a new {@code OAuth2CrestAuthorizationModule} with the provided configuration parameters.
     *
     * @param accessTokenValidator A {@code OAuth2AccessTokenValidator} instance.
     * @param requiredScopes The required OAuth2 scopes for the request to be authorized.
     * @param cacheEnabled {@code true} if the cache should be used.
     * @param cacheSize The size of the cache. Only used if {@code cacheEnabled} is set to {@code true}.
     * @return A new {@code OAuth2CrestAuthorizationModule} instance.
     */
    public static OAuth2CrestAuthorizationModule forCrest(OAuth2AccessTokenValidator accessTokenValidator,
                                                          Set<String> requiredScopes,
                                                          boolean cacheEnabled,
                                                          int cacheSize) {
        return new OAuth2CrestAuthorizationModule(new OAuth2Module(accessTokenValidator,
                                                                   requiredScopes,
                                                                   cacheEnabled,
                                                                   cacheSize),
                                                  new BearerTokenExtractor());
    }

    /**
     * Creates a new {@code OAuth2CrestAuthorizationModule} with the provided configuration parameters.
     *
     * @param httpClientHandler The {@link Handler} to use to make HTTP requests.
     * @param tokenInfoEndpoint The URI for the OAuth2 token info endpoint.
     * @param userInfoEndpoint The URI for the OAuth2 user info endpoint.
     * @param requiredScopes The required OAuth2 scopes for the request to be authorized.
     * @param cacheEnabled {@code true} if the cache should be used.
     * @param cacheSize The size of the cache. Only used if {@code cacheEnabled} is set to {@code true}.
     * @return A new {@code OAuth2CrestAuthorizationModule} instance.
     */
    public static OAuth2CrestAuthorizationModule forCrest(HttpClientHandler httpClientHandler,
                                                          String tokenInfoEndpoint,
                                                          String userInfoEndpoint,
                                                          Set<String> requiredScopes,
                                                          boolean cacheEnabled,
                                                          int cacheSize) {
        return forCrest(new RestOAuth2AccessTokenValidator(
                                json(
                                        object(
                                                field(TOKEN_INFO_ENDPOINT_KEY, tokenInfoEndpoint),
                                                field(USER_INFO_ENDPOINT_KEY, userInfoEndpoint))),
                                new Client(httpClientHandler)),
                        requiredScopes,
                        cacheEnabled,
                        cacheSize);
    }

    /**
     * <p>Creates a new {@code OAuth2CrestAuthorizationModule} with the provided configuration parameters.</p>
     *
     * <p>Disables the cache.</p>
     *
     * @param httpClientHandler The {@link Handler} to use to make HTTP requests.
     * @param tokenInfoEndpoint The URI for the OAuth2 token info endpoint.
     * @param userInfoEndpoint The URI for the OAuth2 user info endpoint.
     * @param requiredScopes The required OAuth2 scopes for the request to be authorized.
     * @return A new {@code OAuth2CrestAuthorizationModule} instance.
     */
    public static OAuth2CrestAuthorizationModule forCrest(HttpClientHandler httpClientHandler,
                                                          String tokenInfoEndpoint,
                                                          String userInfoEndpoint,
                                                          Set<String> requiredScopes) {
        return forCrest(httpClientHandler, tokenInfoEndpoint, userInfoEndpoint, requiredScopes, false, 0);
    }
}
