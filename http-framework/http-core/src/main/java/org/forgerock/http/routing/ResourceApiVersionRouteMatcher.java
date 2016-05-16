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

import java.util.Objects;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;

/**
 * A {@link RouteMatcher} which routes requests using the resource API version
 * matching against the request's {@literal Accept-API-Version} header.
 * Examples of valid versions include:
 *
 * <pre>
 * 1
 * 1.0
 * 2.5
 * 123.123
 * </pre>
 *
 * Routes may be added and removed from a router as follows:
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = RouteMatchers.requestResourceApiVersionMatcher(Version.version(1));
 * RouteMatcher routeTwo = RouteMatchers.requestResourceApiVersionMatcher(Version.version(2, 5));
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre>
 *
 * <p>Default routing behaviour, for when no {@literal Accept-API-Version}
 * header is set on the request, can be selected by specifying a
 * {@link DefaultVersionBehaviour} to the {@link ResourceApiVersionBehaviourManager}
 * instance.
 *
 * <pre>
 * ResourceApiVersionBehaviourManager behaviourManager = RouteMatchers.newResourceApiVersionBehaviourManager();
 * behaviourManager.setDefaultVersionBehaviour(DefaultVersionBehaviour.OLDEST);
 * Filter apiVersionFilter = RouteMatchers.resourceApiVersionContextFilter(behaviourManager);
 * Handler handler = Handlers.chainOf(router, apiVersionFilter);
 * </pre>
 * </p>
 */
class ResourceApiVersionRouteMatcher extends RouteMatcher<Version> {

    private final Version routeVersion;

    /**
     * Creates a new API Version route matcher which will match the given
     * version.
     *
     * @param routeVersion The API version of the resource route.
     */
    ResourceApiVersionRouteMatcher(Version routeVersion) {
        this.routeVersion = routeVersion;
    }

    @Override
    public RouteMatch evaluate(Context context, Version requestedVersion) {
        //TODO should this blow up if ApiVersionRouterContext not present, as that means the filter is not in the route?
        DefaultVersionBehaviour behaviour = getRoutingBehaviour(context);
        if (requestedVersion == null && DefaultVersionBehaviour.NONE.equals(behaviour)) {
            return null;
        } else if (requestedVersion == null) {
            return new ApiVersionRouteMatch(routeVersion, behaviour);
        } else if (routeVersion.isCompatibleWith(requestedVersion)) {
            return new ApiVersionRouteMatch(routeVersion, DefaultVersionBehaviour.NONE);
        } else {
            return null;
        }
    }

    private DefaultVersionBehaviour getRoutingBehaviour(Context context) {
        DefaultVersionBehaviour behaviour = null;
        if (context.containsContext(ApiVersionRouterContext.class)) {
            behaviour = context.asContext(ApiVersionRouterContext.class).getDefaultVersionBehaviour();
        }
        if (behaviour == null) {
            behaviour = DefaultVersionBehaviour.LATEST;
        }
        return behaviour;
    }


    @Override
    public String toString() {
        return routeVersion.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceApiVersionRouteMatcher that = (ResourceApiVersionRouteMatcher) o;
        return Objects.equals(routeVersion, that.routeVersion);
    }

    @Override
    public String idFragment() {
        return ":" + routeVersion.toString();
    }

    @Override
    public <T> T transformApi(T descriptor, ApiProducer<T> producer) {
        return descriptor != null ? producer.withVersion(descriptor, routeVersion) : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeVersion);
    }

    private static final class ApiVersionRouteMatch implements RouteMatch {

        private final Version resourceVersion;
        private final DefaultVersionBehaviour behaviour;

        private ApiVersionRouteMatch(Version resourceVersion, DefaultVersionBehaviour behaviour) {
            this.resourceVersion = resourceVersion;
            this.behaviour = behaviour;
        }

        @Override
        public boolean isBetterMatchThan(RouteMatch routeMatch) throws IncomparableRouteMatchException {
            if (routeMatch == null) {
                return true;
            } else if (!(routeMatch instanceof ApiVersionRouteMatch)) {
                throw new IncomparableRouteMatchException(this, routeMatch);
            }

            ApiVersionRouteMatch result = (ApiVersionRouteMatch) routeMatch;
            switch (behaviour) {
            case OLDEST:
                return resourceVersion.compareTo(result.resourceVersion) <= 0;
            case LATEST:
            default:
                return resourceVersion.compareTo(result.resourceVersion) >= 0;
            }
        }

        @Override
        public Context decorateContext(Context context) {
            ApiVersionRouterContext apiVersionRouterContext;
            if (!context.containsContext(ApiVersionRouterContext.class)) {
                apiVersionRouterContext = new ApiVersionRouterContext(context);
                context = apiVersionRouterContext;
            } else {
                apiVersionRouterContext = context.asContext(ApiVersionRouterContext.class);
            }
            apiVersionRouterContext.setResourceVersion(resourceVersion);
            return context;
        }

        @Override
        public String toString() {
            return "version=" + resourceVersion + ", behaviour=" + behaviour;
        }
    }
}
