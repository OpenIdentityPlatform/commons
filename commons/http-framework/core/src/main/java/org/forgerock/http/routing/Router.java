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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.http.routing;

import static org.forgerock.http.protocol.Responses.newNotFound;
import static org.forgerock.http.routing.RouteMatchers.getRemainingRequestUri;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Handler;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.AbstractRouter;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Swagger;

/**
 * A router which routes requests based on route matchers. Each route is
 * comprised of a {@link RouteMatcher route matcher} and a corresponding
 * handler, when routing a request the router will call
 * {@link RouteMatcher#evaluate(Context, Object)} for each
 * registered route and use the returned {@link RouteMatch} to determine
 * which route best matches the request.
 *
 * <p>Routes may be added and removed from a router as follows:</p>
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = RouteMatchers.requestUriMatcher(EQUALS, &quot;users&quot;);
 * RouteMatcher routeTwo = RouteMatcher.requestUriMatcher(EQUALS, &quot;users/{userId}&quot;);
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre>
 *
 * @see AbstractRouter
 * @see UriRouteMatcher
 * @see RouteMatchers
 */
public final class Router extends AbstractRouter<Router, Request, Handler, Swagger> implements DescribableHandler {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    /** Creates a new router with no routes defined. */
    public Router() {
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router The router to be copied.
     */
    public Router(Router router) {
        super(router);
    }

    @Override
    protected Router getThis() {
        return this;
    }

    @Override
    protected RouteMatcher<Request> uriMatcher(RoutingMode mode, String pattern) {
        return RouteMatchers.requestUriMatcher(mode, pattern);
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        try {
            Pair<Context, Handler> bestMatch = getBestRoute(context, request);
            if (bestMatch != null) {
                return bestMatch.getSecond().handle(bestMatch.getFirst(), request);
            } else {
                return newResultPromise(newNotFound());
            }
        } catch (IncomparableRouteMatchException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Route for '{}' not found", getRemainingRequestUri(context, request));
            }
            return newResultPromise(new ResponseException(e.getMessage()).getResponse());
        }
    }
}
