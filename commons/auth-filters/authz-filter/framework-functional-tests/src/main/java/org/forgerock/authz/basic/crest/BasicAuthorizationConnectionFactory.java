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

package org.forgerock.authz.basic.crest;

import static org.forgerock.authz.filter.crest.AuthorizationFilters.*;
import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.json.resource.RouteMatchers.*;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;

/**
 * Factory class for getting the {@code ConnectionFactory} instance that has all of the CREST routes.
 *
 * @since 1.5.0
 */
public final class BasicAuthorizationConnectionFactory {

    private static final SimpleResource SIMPLE_RESOURCE = new SimpleResource();

    /**
     * Private utility constructor.
     */
    private BasicAuthorizationConnectionFactory() {
        throw new UnsupportedOperationException("BasicAuthorizationConnectionFactory cannot be instantiated.");
    }

    /**
     * Creates a {@link ConnectionFactory} instance with all the (non-)protected endpoints.
     *
     * @return A {@code ConnectionFactory} instance.
     */
    public static ConnectionFactory getConnectionFactory() {

        Router router = new Router();

        router.addRoute(requestUriMatcher(STARTS_WITH, "/simple"), createEndpointCheckerHandler());
        router.addRoute(requestUriMatcher(STARTS_WITH, "/notAction"), createNotActionHandler());
        router.addRoute(requestUriMatcher(STARTS_WITH, "/notCreateOrPatch"), createNotCreateOrPatchHandler());
        router.addRoute(requestUriMatcher(STARTS_WITH, "/none"), createNoneHandler());

        return Resources.newInternalConnectionFactory(router);
    }

    /**
     * Creates an authorization filter request handler which allows requests to the {@code user}s endpoint and denies
     * requests to the {@code role}s endpoint.
     *
     * @return A {@code RequestHandler}.
     */
    private static RequestHandler createEndpointCheckerHandler() {
        Router router = new Router();

        router.addRoute(requestUriMatcher(STARTS_WITH, "/users"), createAuthorizationFilter(SIMPLE_RESOURCE,
                new AlwaysAllowAuthorizationModule()));
        router.addRoute(requestUriMatcher(STARTS_WITH, "/roles"), createAuthorizationFilter(SIMPLE_RESOURCE,
                new AlwaysDenyAuthorizationModule("roles")));

        return router;
    }

    /**
     * Creates an authorization filter request handler which denies action requests.
     *
     * @return A no action {@code RequestHandler}.
     */
    private static RequestHandler createNotActionHandler() {
        return createAuthorizationFilter(SIMPLE_RESOURCE,
                new NotActionAuthorizationModule());
    }

    /**
     * Creates an authorization filter request handler which denies create and patch requests.
     *
     * @return A no create or patch {@code RequestHandler}.
     */
    private static RequestHandler createNotCreateOrPatchHandler() {
        return createAuthorizationFilter(SIMPLE_RESOURCE,
                new NotCreateAuthorizationModule(), new NotPatchAuthorizationModule());
    }

    /**
     * Creates an authorization filter request handler which denies all requests.
     *
     * @return A deny all {@code RequestHandler}.
     */
    private static RequestHandler createNoneHandler() {
        return createAuthorizationFilter(SIMPLE_RESOURCE,
                new NotCreateAuthorizationModule(), new NotReadAuthorizationModule(),
                new NotUpdateAuthorizationModule(), new NotDeleteAuthorizationModule(),
                new NotPatchAuthorizationModule(), new NotActionAuthorizationModule(),
                new NotQueryAuthorizationModule());
    }
}
