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

package org.forgerock.authz.modules.oauth2.crest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import java.util.Collections;

import org.forgerock.authz.basic.crest.SimpleResource;
import org.forgerock.authz.filter.crest.AuthorizationFilters;
import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2AccessTokenValidator;
import org.forgerock.authz.modules.oauth2.OAuth2Authorization;
import org.forgerock.authz.modules.oauth2.OAuth2CrestAuthorizationModule;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;

/**
 * A factory class for creating an {@code ConnectionFactory} configured with an OAuth2 authorization filter.
 *
 * @since 1.5.0
 */
public final class OAuth2AuthorizationConnectionFactory {

    private static final SimpleResource SIMPLE_RESOURCE = new SimpleResource();

    /**
     * Private utility constructor.
     */
    private OAuth2AuthorizationConnectionFactory() {
        throw new UnsupportedOperationException("OAuth2AuthorizationConnectionFactory cannot be instantiated.");
    }

    /**
     * Creates a {@code ConnectionFactory} with a route to the "resource" endpoint, which is protected by the
     * OAuth2 authorization filter which will deem an access token valid if it matches the string "VALID" and will
     * return a scope of "SCOPE" and user info of "UID"->"DEMO".
     *
     * @return A {@code ConnectionFactory} instance.
     */
    public static ConnectionFactory getConnectionFactory() {

        OAuth2CrestAuthorizationModule authorizationModule = OAuth2Authorization.forCrest(
                new OAuth2AccessTokenValidator() {
                    @Override
                    public AccessTokenValidationResponse validate(String accessToken) {
                        if ("VALID".equalsIgnoreCase(accessToken)) {
                            return new AccessTokenValidationResponse(System.currentTimeMillis() + 5000,
                                    Collections.<String, Object>singletonMap("UID", "DEMO"),
                                    Collections.singleton("SCOPE"));
                        } else {
                            return new AccessTokenValidationResponse(0);
                        }
                    }
                }, Collections.<String>emptySet(), false, 0);

        Router router = new Router();

        router.addRoute(requestUriMatcher(STARTS_WITH, "/resource"), AuthorizationFilters.createFilter(SIMPLE_RESOURCE,
                authorizationModule));

        return Resources.newInternalConnectionFactory(router);
    }
}
