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
 * Copyright 2012-2014 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.json.resource.Requests.copyOfActionRequest;
import static org.forgerock.json.resource.Requests.copyOfCreateRequest;
import static org.forgerock.json.resource.Requests.copyOfDeleteRequest;
import static org.forgerock.json.resource.Requests.copyOfPatchRequest;
import static org.forgerock.json.resource.Requests.copyOfQueryRequest;
import static org.forgerock.json.resource.Requests.copyOfReadRequest;
import static org.forgerock.json.resource.Requests.copyOfUpdateRequest;
import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.UriRoute.RouteMatcher;

/**
 * A request handler which routes requests using URI template matching against
 * the request's resource name. Examples of valid URI templates include:
 *
 * <pre>
 * users
 * users/{userId}
 * users/{userId}/devices
 * users/{userId}/devices/{deviceId}
 * </pre>
 *
 * Routes may be added and removed from a router as follows:
 *
 * <pre>
 * RequestHandler users = ...;
 * Router router = new Router();
 * Route r1 = router.addRoute(EQUALS, &quot;users&quot;, users);
 * Route r2 = router.addRoute(EQUALS, &quot;users/{userId}&quot;, users);
 *
 * // Deregister a route.
 * router.removeRoute(r1, r2);
 * </pre>
 *
 * A request handler receiving a routed request may access the associated
 * route's URI template variables via
 * {@link RouterContext#getUriTemplateVariables()}. For example, a request
 * handler processing requests for the route users/{userId} may obtain the value
 * of {@code userId} as follows:
 *
 * <pre>
 * String userId = context.asContext(RouterContext.class).getUriTemplateVariables().get(&quot;userId&quot;);
 * </pre>
 *
 * During routing resource names are "relativized" by removing the leading path
 * components which matched the template. See the documentation for
 * {@link RouterContext} for more information.
 * <p>
 * <b>NOTE:</b> for simplicity this implementation only supports a small sub-set
 * of the functionality described in RFC 6570.
 *
 * @see RouterContext
 * @see UriRouter
 * @see <a href="http://tools.ietf.org/html/rfc6570">RFC 6570 - URI Template
 *      </a>
 */
public final class UriRouter implements RequestHandler {

    private volatile RequestHandler defaultRoute = null;
    private final Set<UriRoute> routes = new CopyOnWriteArraySet<UriRoute>();

    /**
     * Creates a new router with no routes defined.
     */
    public UriRouter() {
        // Nothing to do.
    }

    /**
     * Adds all of the routes defined in the provided router to this router. New
     * routes may be added while this router is processing requests.
     *
     * @param router
     *            The router whose routes are to be copied into this router.
     * @return This router.
     */
    UriRouter addAllRoutes(final UriRouter router) {
        if (this != router) {
            routes.addAll(router.routes);
        }
        return this;
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
     * UriRouter router = new UriRouter();
     *
     * // This is valid usage: the template matches the resource collection.
     * router.addRoute("users", users);
     *
     * // This is invalid usage: the template matches resource instances.
     * router.addRoute("users/{userId}", users);
     * </pre>
     *
     * @param uriTemplate
     *            The URI template which request resource names must match.
     * @param provider
     *            The collection resource provider to which matching requests
     *            will be routed.
     * @return An opaque handle for the route which may be used for removing the
     *         route later.
     */
    UriRoute addRoute(final String uriTemplate,
            final CollectionResourceProvider provider) {
        return addRoute(STARTS_WITH, uriTemplate, newCollection(provider));
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
     * @return An opaque handle for the route which may be used for removing the
     *         route later.
     */
    public UriRoute addRoute(final RoutingMode mode, final String uriTemplate,
            final RequestHandler handler) {
        return addRoute(new UriRoute(mode, uriTemplate, handler));
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
     * @return An opaque handle for the route which may be used for removing the
     *         route later.
     */
    UriRoute addRoute(final String uriTemplate, final SingletonResourceProvider provider) {
        return addRoute(EQUALS, uriTemplate, newSingleton(provider));
    }

    /**
     * Returns the request handler to be used as the default route for requests
     * which do not match any of the other defined routes.
     *
     * @return The request handler to be used as the default route.
     */
    RequestHandler getDefaultRoute() {
        return defaultRoute;
    }

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final ActionRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfActionRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleAction(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final CreateRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfCreateRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleCreate(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final DeleteRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfDeleteRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleDelete(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final PatchRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfPatchRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handlePatch(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final QueryRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfQueryRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleQuery(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final ReadRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfReadRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleRead(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RouteMatcher bestMatch = getBestRoute(context, request);
            final UpdateRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfUpdateRequest(request).setResourceName(bestMatch.getRemaining())
                    : request;
            bestMatch.getRequestHandler().handleUpdate(bestMatch.getServerContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Removes all of the routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @return This router.
     */
    UriRouter removeAllRoutes() {
        routes.clear();
        return this;
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while
     * this router is processing requests.
     *
     * @param routes
     *            The routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    boolean removeRoute(final UriRoute... routes) {
        boolean isModified = false;
        for (final UriRoute route : routes) {
            isModified |= this.routes.remove(route);
        }
        return isModified;
    }

    /**
     * Sets the request handler to be used as the default route for requests
     * which do not match any of the other defined routes.
     *
     * @param handler
     *            The request handler to be used as the default route.
     * @return This router.
     */
    UriRouter setDefaultRoute(final RequestHandler handler) {
        this.defaultRoute = handler;
        return this;
    }

    UriRoute addRoute(final UriRoute route) {
        routes.add(route);
        return route;
    }

    private RouteMatcher getBestRoute(final ServerContext context, final Request request)
            throws ResourceException {
        RouteMatcher bestMatcher = null;
        for (final UriRoute route : routes) {
            final RouteMatcher matcher = route.getRouteMatcher(context, request);
            if (matcher != null && matcher.isBetterMatchThan(bestMatcher)) {
                bestMatcher = matcher;
            }
        }
        if (bestMatcher != null) {
            return bestMatcher;
        }
        final RequestHandler handler = defaultRoute;

        /*
         * Passing the resourceName through explicitly means if an incorrect version was requested the error returned
         * is specific to the endpoint requested.
         */
        if (handler != null) {
            return new RouteMatcher(context, handler, request.getResourceName());
        }
        // TODO: i18n
        throw new NotFoundException(String.format("Resource '%s' not found", request
                .getResourceName()));
    }
}
