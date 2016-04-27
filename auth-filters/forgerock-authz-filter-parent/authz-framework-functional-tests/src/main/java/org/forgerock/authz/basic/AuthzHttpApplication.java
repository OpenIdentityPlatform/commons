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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.authz.basic;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collections;

import org.forgerock.authz.basic.crest.BasicAuthorizationConnectionFactory;
import org.forgerock.authz.basic.http.AuthorizationContextHandler;
import org.forgerock.authz.basic.http.EndpointChecker;
import org.forgerock.authz.basic.http.HttpEndpointCheckerAuthorizationModule;
import org.forgerock.authz.filter.http.HttpAuthorizationFilter;
import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2AccessTokenValidator;
import org.forgerock.authz.modules.oauth2.OAuth2Authorization;
import org.forgerock.authz.modules.oauth2.OAuth2Exception;
import org.forgerock.authz.modules.oauth2.crest.OAuth2AuthorizationConnectionFactory;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.Router;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.Promise;

/**
 * HTTP Application for authorization framework functional tests.
 *
 * @since 2.0.0
 */
public class AuthzHttpApplication implements HttpApplication {

    @Override
    public Handler start() throws HttpApplicationException {
        Router router = new Router();
        router.addRoute(requestUriMatcher(STARTS_WITH, "basic/crest"),
                CrestHttp.newHttpHandler(BasicAuthorizationConnectionFactory.getConnectionFactory()));
        router.addRoute(requestUriMatcher(STARTS_WITH, "modules/oauth2/crest"),
                CrestHttp.newHttpHandler(OAuth2AuthorizationConnectionFactory.getConnectionFactory()));


        router.addRoute(requestUriMatcher(STARTS_WITH, "basic/http"),
                Handlers.chainOf(new AuthorizationContextHandler(),
                        new HttpAuthorizationFilter(new HttpEndpointCheckerAuthorizationModule(
                                new EndpointChecker()))));
        router.addRoute(requestUriMatcher(STARTS_WITH, "modules/oauth2/http"),
                Handlers.chainOf(new AuthorizationContextHandler(),
                        new HttpAuthorizationFilter(OAuth2Authorization.forHttp(new OAuth2AccessTokenValidator() {
                            @Override
                            public Promise<AccessTokenValidationResponse, OAuth2Exception> validate(String accessToken) {
                                if ("VALID".equalsIgnoreCase(accessToken)) {
                                    return newResultPromise(
                                            new AccessTokenValidationResponse(System.currentTimeMillis() + 5000,
                                            Collections.<String, Object>singletonMap("UID", "DEMO"),
                                            Collections.singleton("SCOPE")));
                                } else {
                                    return newResultPromise(new AccessTokenValidationResponse(0));
                                }
                            }
                        }, Collections.<String>emptySet(), false, 0))));
        return router;
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}
