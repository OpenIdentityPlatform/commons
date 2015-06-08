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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.http.RoutingMode.EQUALS;
import static org.forgerock.http.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.forgerock.http.AbstractUriRouter;
import org.forgerock.http.RouteMatcher;
import org.forgerock.http.RouteNotFoundException;
import org.forgerock.http.ServerContext;
import org.forgerock.http.UriRoute;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

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
 * {@link org.forgerock.http.RouterContext#getUriTemplateVariables()}. For example, a request
 * handler processing requests for the route users/{userId} may obtain the value
 * of {@code userId} as follows:
 *
 * <pre>
 * String userId = context.asContext(RouterContext.class).getUriTemplateVariables().get(&quot;userId&quot;);
 * </pre>
 *
 * During routing resource names are "relativized" by removing the leading path
 * components which matched the template. See the documentation for
 * {@link org.forgerock.http.RouterContext} for more information.
 * <p>
 * <b>NOTE:</b> for simplicity this implementation only supports a small sub-set
 * of the functionality described in RFC 6570.
 *
 * @see org.forgerock.http.RouterContext
 * @see <a href="http://tools.ietf.org/html/rfc6570">RFC 6570 - URI Template
 *      </a>
 */
public final class UriRouter extends AbstractUriRouter<UriRouter, RequestHandler> implements RequestHandler {

    /**
     * Creates a new router with no routes defined.
     */
    public UriRouter() {
        // Nothing to do.
    }

    /**
     * Creates a new router containing the same routes and default route as the
     * provided router. Changes to the returned router's routing table will not
     * impact the provided router.
     *
     * @param router
     *            The router to be copied.
     */
    public UriRouter(final UriRouter router) {
        super(router);
    }

    @Override
    protected UriRouter getThis() {
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
     *            The URI template which request resource paths must match.
     * @param provider
     *            The collection resource provider to which matching requests
     *            will be routed.
     * @return An opaque handle for the route which may be used for removing the
     *         route later.
     */
    public UriRoute<RequestHandler> addRoute(final String uriTemplate,
            final CollectionResourceProvider provider) {
        return addRoute(STARTS_WITH, uriTemplate, newCollection(provider));
    }

    /**
     * Adds a new route to this router for the provided singleton resource
     * provider. New routes may be added while this router is processing
     * requests.
     *
     * @param uriTemplate
     *            The URI template which request resource paths must match.
     * @param provider
     *            The singleton resource provider to which matching requests
     *            will be routed.
     * @return An opaque handle for the route which may be used for removing the
     *         route later.
     */
    public UriRoute<RequestHandler> addRoute(final String uriTemplate, final SingletonResourceProvider provider) {
        return addRoute(EQUALS, uriTemplate, newSingleton(provider));
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(final ServerContext context,
            final ActionRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final ActionRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfActionRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleAction(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleCreate(final ServerContext context, final CreateRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final CreateRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfCreateRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleCreate(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleDelete(final ServerContext context, final DeleteRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final DeleteRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfDeleteRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleDelete(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handlePatch(final ServerContext context, final PatchRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final PatchRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfPatchRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handlePatch(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResult, ResourceException> handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final QueryRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfQueryRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleQuery(bestMatch.getContext(), routedRequest, handler);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(final ServerContext context, final ReadRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final ReadRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfReadRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleRead(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleUpdate(final ServerContext context, final UpdateRequest request) {
        try {
            final RouteMatcher<RequestHandler> bestMatch = getBestRoute(context, request);
            final UpdateRequest routedRequest = bestMatch.wasRouted()
                    ? copyOfUpdateRequest(request).setResourcePath(bestMatch.getRemaining())
                    : request;
            return bestMatch.getHandler().handleUpdate(bestMatch.getContext(), routedRequest);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    private RouteMatcher<RequestHandler> getBestRoute(final ServerContext context, final Request request)
            throws ResourceException {
        try {
            return getBestRoute(context, request.getResourcePath());
        } catch (RouteNotFoundException e) {
            // TODO: i18n
            throw new NotFoundException(String.format("Resource '%s' not found", request.getResourcePath()));
        }
    }
}
