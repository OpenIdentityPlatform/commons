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

package org.forgerock.json.resource;

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.ResourceApiVersionRoutingFilter.*;
import static org.forgerock.json.resource.Resources.*;
import static org.forgerock.json.resource.RouteMatchers.*;
import static org.forgerock.util.promise.Promises.*;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.services.routing.AbstractRouter;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

/**
 * A router which routes requests based on route predicates. Each route is
 * comprised of a {@link RouteMatcher route matcher} and a corresponding
 * handler, when routing a request the router will call
 * {@link RouteMatcher#evaluate} for each
 * registered route and use the returned {@link RouteMatcher} to determine
 * which route best matches the request.
 *
 * <p>Routes may be added and removed from a router as follows:
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = RouteMatchers.requestUriMatcher(EQUALS, &quot;users&quot;);
 * RouteMatcher routeTwo = RouteMatchers.requestUriMatcher(EQUALS, &quot;users/{userId}&quot;);
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre>
 *
 * @see AbstractRouter
 * @see RouteMatchers
 */
public class Router extends AbstractRouter<Router, Request, RequestHandler, ApiDescription>
        implements RequestHandler {

    /**
     * Creates a new router with no routes defined.
     */
    public Router() {
        super();
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router The router to be copied.
     */
    public Router(AbstractRouter<Router, Request, RequestHandler, ApiDescription> router) {
        super(router);
    }

    @Override
    protected Router getThis() {
        return this;
    }

    @Override
    protected RouteMatcher<Request> uriMatcher(RoutingMode mode, String pattern) {
        return requestUriMatcher(mode, pattern);
    }

    /**
     * Adds a new route to this router for the provided collection resource
     * provider. New routes may be added while this router is processing
     * requests.
     * <p>
     * The provided URI template must match the resource collection itself, not
     * resource instances. For example:
     *
     * <pre>
     * CollectionResourceProvider users = ...;
     * Router router = new Router();
     *
     * // This is valid usage: the template matches the resource collection.
     * router.addRoute(Router.uriTemplate("users"), users);
     *
     * // This is invalid usage: the template matches resource instances.
     * router.addRoute(Router.uriTemplate("users/{userId}"), users);
     * </pre>
     *
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param provider
     *            The collection resource provider to which matching requests
     *            will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(UriTemplate uriTemplate, CollectionResourceProvider provider) {
        RouteMatcher<Request> routeMatcher = requestUriMatcher(STARTS_WITH, uriTemplate.template);
        addRoute(routeMatcher, newHandler(provider));
        return routeMatcher;
    }

    /**
     * Adds a new route to this router for the provided singleton resource
     * provider. New routes may be added while this router is processing
     * requests.
     *
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param provider
     *            The singleton resource provider to which matching requests
     *            will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(UriTemplate uriTemplate, SingletonResourceProvider provider) {
        RouteMatcher<Request> routeMatcher = requestUriMatcher(EQUALS, uriTemplate.template);
        addRoute(routeMatcher, newHandler(provider));
        return routeMatcher;
    }

    /**
     * Adds a new route to this router for the provided request handler. New
     * routes may be added while this router is processing requests.
     *
     * @param mode
     *            Indicates how the URI template should be matched against
     *            resource names.
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param handler
     *            The request handler to which matching requests will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(RoutingMode mode, UriTemplate uriTemplate, RequestHandler handler) {
        RouteMatcher<Request> routeMatcher = requestUriMatcher(mode, uriTemplate.template);
        addRoute(routeMatcher, handler);
        return routeMatcher;
    }

    /**
     * Creates a {@link UriTemplate} from a URI template string that will be
     * used to match and route incoming requests.
     *
     * @param template The URI template.
     * @return A {@code UriTemplate} instance.
     */
    public static UriTemplate uriTemplate(String template) {
        return new UriTemplate(template);
    }

    /**
     * Adds a new route to this router for the provided collection resource
     * provider. New routes may be added while this router is processing
     * requests.
     *
     * @param version The resource API version the the request must match.
     * @param provider The collection resource provider to which matching
     *                 requests will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(Version version, CollectionResourceProvider provider) {
        return addRoute(version, newHandler(provider));
    }

    /**
     * Adds a new route to this router for the provided singleton resource
     * provider. New routes may be added while this router is processing
     * requests.
     *
     * @param version The resource API version the the request must match.
     * @param provider The singleton resource provider to which matching
     *                 requests will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(Version version, SingletonResourceProvider provider) {
        return addRoute(version, newHandler(provider));
    }

    /**
     * Adds a new route to this router for the provided request handler. New
     * routes may be added while this router is processing requests.
     *
     * @param version The resource API version the the request must match.
     * @param handler
     *            The request handler to which matching requests will be routed.
     * @return The {@link RouteMatcher} for the route that can be used to
     * remove the route at a later point.
     */
    public RouteMatcher<Request> addRoute(Version version, RequestHandler handler) {
        RouteMatcher<Request> routeMatcher = requestResourceApiVersionMatcher(version);
        addRoute(routeMatcher, handler);
        return routeMatcher;
    }

    private Pair<Context, RequestHandler> getBestMatch(Context context, Request request)
            throws ResourceException {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestRoute(context, request);
            if (bestMatch == null) {
                throw new NotFoundException(String.format("Resource '%s' not found", request.getResourcePath()));
            }
            return bestMatch;
        } catch (IncomparableRouteMatchException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            ActionRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfActionRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleAction(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            CreateRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfCreateRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleCreate(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            DeleteRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfDeleteRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleDelete(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            PatchRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfPatchRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handlePatch(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            final Context decoratedContext = bestMatch.getFirst();
            UriRouterContext routerContext = getRouterContext(decoratedContext);
            QueryRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfQueryRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            QueryResourceHandler resourceHandler = new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    if (decoratedContext.containsContext(ApiVersionRouterContext.class)) {
                        ApiVersionRouterContext apiVersionRouterContext =
                                decoratedContext.asContext(ApiVersionRouterContext.class);
                        setApiVersionInfo(apiVersionRouterContext, request, resource);
                    }
                    return handler.handleResource(resource);
                }
            };
            return bestMatch.getSecond().handleQuery(decoratedContext, routedRequest, resourceHandler);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            ReadRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfReadRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleRead(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            Pair<Context, RequestHandler> bestMatch = getBestMatch(context, request);
            UriRouterContext routerContext = getRouterContext(bestMatch.getFirst());
            UpdateRequest routedRequest = wasRouted(context, routerContext)
                    ? copyOfUpdateRequest(request).setResourcePath(getResourcePath(routerContext))
                    : request;
            return bestMatch.getSecond().handleUpdate(bestMatch.getFirst(), routedRequest);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApiDescription handleApiRequest(Context context, Request request) {
        try {
            Pair<Context, RequestHandler> bestRoute = getBestRoute(context, request);
            if (bestRoute != null) {
                RequestHandler handler = bestRoute.getSecond();
                if (handler instanceof Describable) {
                    Context nextContext = bestRoute.getFirst();
                    UriRouterContext routerContext = getRouterContext(nextContext);
                    Request routedRequest = wasRouted(context, routerContext)
                        ? copyOfApiRequest(request).setResourcePath(getResourcePath(routerContext))
                        : request;
                    return ((Describable<ApiDescription, Request>) handler)
                        .handleApiRequest(nextContext, routedRequest);
                }
            }
        } catch (IncomparableRouteMatchException e) {
            throw new IllegalStateException(e);
        }
        if (thisRouterUriMatcher.evaluate(context, request) != null) {
            return this.api;
        }
        throw new UnsupportedOperationException(
            "No route matched the request resource path " + request.getResourcePath());
    }

    private UriRouterContext getRouterContext(Context context) {
        return context.containsContext(UriRouterContext.class)
            ? context.asContext(UriRouterContext.class)
            : null;
    }

    private boolean wasRouted(Context originalContext, UriRouterContext routerContext) {
        return routerContext != null
                && (!originalContext.containsContext(UriRouterContext.class)
                || routerContext != originalContext.asContext(UriRouterContext.class));
    }

    private String getResourcePath(UriRouterContext routerContext) {
        return routerContext.getRemainingUri();
    }

    /**
     * Represents a URI template string that will be used to match and route
     * incoming requests.
     */
    public static final class UriTemplate {
        private final String template;

        private UriTemplate(String template) {
            this.template = template;
        }

        /**
         * Return the string representation of the UriTemplate.
         *
         * @return the string representation of the UriTemplate.
         */
        @Override
        public String toString() {
            return template;
        }
    }
}
