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

package org.forgerock.http.routing;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.http.Context;
import org.forgerock.util.Pair;

/**
 * A router which routes requests based on route matchers. Each route is
 * comprised of a {@link RouteMatcher route matcher} and a corresponding
 * handler, when routing a request the router will call
 * {@link RouteMatcher#evaluate(Context, Object)} for each registered route
 * and use the returned {@link RouteMatch} to determine which route best
 * matches the request.
 *
 * <p>Routes may be added and removed from a router as follows:</p>
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = new UriRouteMatcher(EQUALS, &quot;users&quot;);
 * RouteMatcher routeTwo = new UriRouteMatcher(EQUALS, &quot;users/{userId}&quot;);
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre>
 *
 * @see Router
 * @see UriRouteMatcher
 * @see RouteMatchers
 *
 * @param <T> The type of the router.
 * @param <R> The type of the request.
 * @param <H> The type of the handler that will be used to handle routing requests.
 */
public abstract class AbstractRouter<T extends AbstractRouter<T, R, H>, R, H> {

    private final Map<RouteMatcher<R>, H> routes = new ConcurrentHashMap<>();
    private volatile H defaultRoute;

    /**
     * Creates a new router with no routes defined.
     */
    protected AbstractRouter() {
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router The router to be copied.
     */
    @SuppressWarnings("unchecked")
    protected AbstractRouter(AbstractRouter<T, R, H> router) {
        this.defaultRoute = router.defaultRoute;
        addAllRoutes((T) router);
    }

    /**
     * Returns this {@code AbstractUriRouter} instance, typed correctly.
     *
     * @return This {@code AbstractUriRouter} instance.
     */
    protected abstract T getThis();

    /**
     * Gets all registered routes on this router.
     *
     * @return All registered routes.
     */
    final Map<RouteMatcher<R>, H> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    /**
     * Adds all of the routes defined in the provided router to this router.
     * New routes may be added while this router is processing requests.
     *
     * @param router The router whose routes are to be copied into this router.
     * @return This router instance.
     */
    public final T addAllRoutes(T router) {
        if (this != router) {
            for (Map.Entry<RouteMatcher<R>, H> route : router.getRoutes().entrySet()) {
                addRoute(route.getKey(), route.getValue());
            }
        }
        return getThis();
    }

    /**
     * Adds a new route to this router for the provided handler. New routes may
     * be added while this router is processing requests.
     *
     * <p>The provided {@literal matcher} can be used to remove this route
     * later.</p>
     *
     * @param matcher The {@code RouteMatcher} that will evaluate whether
     *                  the incoming request matches this route.
     * @param handler The handler to which matching requests will be routed.
     * @return This router instance.
     */
    public final T addRoute(RouteMatcher<R> matcher, H handler) {
        routes.put(matcher, handler);
        return getThis();
    }

    /**
     * Sets the handler to be used as the default route for requests which do
     * not match any of the other defined routes.
     *
     * @param handler The handler to be used as the default route.
     * @return This router instance.
     */
    public final T setDefaultRoute(H handler) {
        this.defaultRoute = handler;
        return getThis();
    }

    /**
     * Returns the handler to be used as the default route for requests which
     * do not match any of the other defined routes.
     *
     * @return The handler to be used as the default route.
     */
    public final H getDefaultRoute() {
        return defaultRoute;
    }

    /**
     * Removes all of the routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @return This router instance.
     */
    public final T removeAllRoutes() {
        routes.clear();
        return getThis();
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @param routes The {@code RouteMatcher}s of the routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    @SafeVarargs
    public final boolean removeRoute(RouteMatcher<R>... routes) {
        boolean isModified = false;
        for (RouteMatcher<R> route : routes) {
            isModified |= this.routes.remove(route) != null;
        }
        return isModified;
    }

    /**
     * Finds the best route that matches the given request based on the route
     * matchers of the registered routes. If no registered route matches at
     * all then the default route is chosen, if present.
     *
     * @param context The request context.
     * @param request The request to be matched against the registered routes.
     * @return A {@code Pair} containing the decorated {@code Context} and the
     * handler which is the best match for the given request or {@code null} if
     * no route was found.
     * @throws IncomparableRouteMatchException If any of the registered
     * {@code RouteMatcher}s could not be compared to one another.
     */
    protected Pair<Context, H> getBestRoute(Context context, R request) throws IncomparableRouteMatchException {
        H handler = null;
        RouteMatch bestMatch = null;
        for (Map.Entry<RouteMatcher<R>, H> route : routes.entrySet()) {
            RouteMatch result = route.getKey().evaluate(context, request);
            if (result != null) {
                if (result.isBetterMatchThan(bestMatch)) {
                    handler = route.getValue();
                    bestMatch = result;
                }
            }
        }
        if (bestMatch != null) {
            return Pair.of(bestMatch.decorateContext(context), handler);
        }

        handler = defaultRoute;
        if (handler != null) {
            return Pair.of(context, handler);
        }
        return null;
    }
}
