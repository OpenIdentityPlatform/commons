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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import static org.forgerock.authz.modules.oauth2.OAuth2Authorization.TOKEN_INFO_ENDPOINT_KEY;
import static org.forgerock.authz.modules.oauth2.OAuth2Authorization.USER_INFO_ENDPOINT_KEY;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access Token Validator for validating OAuth2 tokens issued by an OAuth2 Provider using REST requests.
 * <br/>
 * This validator requires the configuration given at construction to contain the following entries:
 * <ul>
 *     <li>token-info-endpoint - the URI of OAuth2 Provider's tokeninfo endpoint (not including the access_token query
 *     parameter</li>
 *     <li>user-info-endpoint - the URI of OAuth2 Provider's userinfo endpoint</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class RestOAuth2AccessTokenValidator implements OAuth2AccessTokenValidator {

    private final Logger logger = LoggerFactory.getLogger(RestOAuth2AccessTokenValidator.class);

    private final Client httpClient;
    private final String tokenInfoEndpoint;
    private final String userProfileEndpoint;


    /**
     * Creates a new instance of the RestOAuth2AccessTokenValidator.
     *
     * @param config The configuration for the validator.
     * @param httpClient The {@link Handler} to use to make HTTP requests.
     */
    public RestOAuth2AccessTokenValidator(JsonValue config, Client httpClient) {
        tokenInfoEndpoint = config.get(TOKEN_INFO_ENDPOINT_KEY).required().asString();
        // userInfo endpoint is optional
        userProfileEndpoint = config.get(USER_INFO_ENDPOINT_KEY).asString();
        this.httpClient = httpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AccessTokenValidationResponse, OAuth2Exception> validate(final String accessToken) {
        Request request = new Request()
                .setMethod("GET")
                .setUri(URI.create(tokenInfoEndpoint + "?access_token=" + accessToken));
        return httpClient.send(request)
                .thenAsync(new AsyncFunction<Response, AccessTokenValidationResponse, OAuth2Exception>() {
                    @Override
                    public Promise<AccessTokenValidationResponse, OAuth2Exception> apply(Response response)
                            throws OAuth2Exception {
                        try {
                            if (response.getStatus().isClientError()) {
                                return newResultPromise(new AccessTokenValidationResponse(0));
                            } else if (!response.getStatus().isSuccessful()) {
                                logger.error(response.getEntity().getString());
                                throw new OAuth2Exception(response.getEntity().getString());
                            }

                            JsonValue tokenInfo = json(response.getEntity().getJson());

                            // If response contains "error" then token is invalid
                            if (tokenInfo.isDefined("error")) {
                                return newResultPromise(new AccessTokenValidationResponse(0));
                            }

                            // expires_in is expressed in seconds, and we compare it later with milliseconds since epoch
                            long expiresIn = tokenInfo.get("expires_in").required().asLong() * 1_000;
                            Set<String> scopes = getScope(tokenInfo);
                            if (userProfileEndpoint != null && expiresIn > 0) {
                                return augmentWithUserProfile(accessToken, expiresIn, scopes);
                            } else {
                                return newResultPromise(new AccessTokenValidationResponse(
                                        expiresIn + System.currentTimeMillis(), new HashMap<String, Object>(), scopes));
                            }
                        } catch (IOException e) {
                            throw new OAuth2Exception(e.getMessage(), e);
                        }
                    }
                }, new AsyncFunction<NeverThrowsException, AccessTokenValidationResponse, OAuth2Exception>() {
                    @Override
                    public Promise<AccessTokenValidationResponse, OAuth2Exception> apply(NeverThrowsException e) {
                        //This can never happen but the exception handler is needed
                        // to change the types of the returned Promise.
                        throw new IllegalStateException("HTTP Client threw a NeverThrowsException?!");
                    }
                });
    }

    private Promise<AccessTokenValidationResponse, OAuth2Exception> augmentWithUserProfile(String accessToken,
            final long expiresIn, final Set<String> scopes) {
        logger.debug("Fetching user profile information from endpoint");
        Request request = new Request()
                .setMethod("GET")
                .setUri(URI.create(userProfileEndpoint));
        request.getHeaders().put("Authorization", "Bearer " + accessToken);
        return httpClient.send(request)
                .then(new Function<Response, AccessTokenValidationResponse, OAuth2Exception>() {
                    @Override
                    public AccessTokenValidationResponse apply(Response response) throws OAuth2Exception {
                        try {
                            if (response.getStatus().isClientError()) {
                                return new AccessTokenValidationResponse(0);
                            } else if (!response.getStatus().isSuccessful()) {
                                logger.error(response.getEntity().getString());
                                throw new OAuth2Exception(response.getEntity().getString());
                            }
                            JsonValue userProfile = json(response.getEntity().getJson());
                            Map<String, Object> profileInfo = new HashMap<>();
                            profileInfo.putAll(userProfile.asMap());
                            return new AccessTokenValidationResponse(expiresIn + System.currentTimeMillis(),
                                    profileInfo, scopes);
                        } catch (IOException e) {
                            throw new OAuth2Exception(e.getMessage(), e);
                        }
                    }
                }, Responses.<AccessTokenValidationResponse, OAuth2Exception>noopExceptionFunction());
    }

    /**
     * Gets the scopes for the access token.
     *
     * @param tokenInfo The response from the token info endpoint.
     * @return The Set of scopes.
     */
    protected Set<String> getScope(JsonValue tokenInfo) {
        final JsonValue scope = tokenInfo.get("scope").required();
        // Some identity Providers are returning the "scope" attribute as an array of string
        // where some others are using a simple space-delimited string
        if (scope.isString()) {
            return new HashSet<>(Arrays.asList(scope.asString().split(" ")));
        }
        return new HashSet<>(scope.asList(String.class));
    }
}
