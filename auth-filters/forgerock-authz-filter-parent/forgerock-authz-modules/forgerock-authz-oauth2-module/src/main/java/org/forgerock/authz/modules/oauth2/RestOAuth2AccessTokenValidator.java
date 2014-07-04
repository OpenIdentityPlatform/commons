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

import org.forgerock.json.fluent.JsonValue;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.authz.modules.oauth2.OAuth2Authorization.TOKEN_INFO_ENDPOINT_KEY;
import static org.forgerock.authz.modules.oauth2.OAuth2Authorization.USER_INFO_ENDPOINT_KEY;

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

    private final RestResourceFactory restResourceFactory;
    private final JsonParser jsonParser;
    private final String tokenInfoEndpoint;
    private final String userProfileEndpoint;

    /**
     * Creates a new instance of the RestOAuth2AccessTokenValidator.
     *
     * @param config The configuration for the validator.
     */
    public RestOAuth2AccessTokenValidator(JsonValue config) {
        this(config, new RestResourceFactory(), new JsonParser());
    }

    /**
     * Constructor for test usage.
     *
     * @param config The configuration for the validator.
     * @param restResourceFactory An instance of the RestResourceFactory.
     * @param jsonParser An instance of the JsonParser.
     */
    RestOAuth2AccessTokenValidator(JsonValue config, RestResourceFactory restResourceFactory, JsonParser jsonParser) {
        tokenInfoEndpoint = config.get(TOKEN_INFO_ENDPOINT_KEY).required().asString();
        // userInfo endpoint is optional
        userProfileEndpoint = config.get(USER_INFO_ENDPOINT_KEY).asString();
        this.restResourceFactory = restResourceFactory;
        this.jsonParser = jsonParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenValidationResponse validate(String accessToken) {

        RestResource tokenInfoRequest = restResourceFactory
                .resource(tokenInfoEndpoint + "?access_token=" + accessToken);

        try {
            final Representation tokenInfoResponse = tokenInfoRequest.get();

            final JsonValue tokenInfo = jsonParser.parse(tokenInfoResponse.getText());

            // If response contains "error" then token is invalid
            if (tokenInfo.isDefined("error")) {
                return new AccessTokenValidationResponse(0);
            }

            // expires_in is expressed in seconds, and we compare it later with milliseconds since epoch
            final long expiresIn = tokenInfo.get("expires_in").required().asLong() * 1000;
            final Set<String> scopes = getScope(tokenInfo);

            final Map<String, Object> profileInfo = new HashMap<String, Object>();
            if (userProfileEndpoint != null && expiresIn > 0) {
                logger.debug("Fetching user profile information from endpoint");
                final RestResource userProfileRequest = restResourceFactory.resource(userProfileEndpoint);
                userProfileRequest.addHeader("Authorization", "Bearer " + accessToken);
                final Representation userProfileResponse = userProfileRequest.get();
                final JsonValue userProfile = jsonParser.parse(userProfileResponse.getText());
                profileInfo.putAll(userProfile.asMap());
            }

            return new AccessTokenValidationResponse(expiresIn + System.currentTimeMillis(), profileInfo, scopes);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new OAuth2Exception(e.getMessage(), e);
        } catch (ResourceException e) {
            if (e.getStatus().getCode() >= 400 && e.getStatus().getCode() < 500) {
                return new AccessTokenValidationResponse(0);
            }
            logger.error(e.getMessage(), e);
            throw new OAuth2Exception(e.getMessage(), e);
        }
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
            return new HashSet<String>(Arrays.asList(scope.asString().split(" ")));
        }
        return new HashSet<String>(scope.asList(String.class));
    }
}
