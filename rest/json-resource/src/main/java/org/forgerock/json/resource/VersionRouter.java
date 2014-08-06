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

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;

/**
 * <p>A request handler which routes requests using URI template matching against the request's resource name and based
 * on a version of a resource.</p>
 *
 * <p>Examples of valid URI templates include:</p>
 *
 * <pre>
 * users
 * users/{userId}
 * users/{userId}/devices
 * users/{userId}/devices/{deviceId}
 * </pre>
 *
 * <p>Routes may be added to the router as follows:</p>
 *
 * <pre>
 * RequestHandler usersV1Dot0 = ...;
 * RequestHandler usersV1Dot5 = ...;
 * Router router = new Router();
 *
 * router.addRoute(&quot;users&quot;)
 * .addVersion(EQUALS, &quot;1.0&quot;, usersV1Dot0)
 * .addVersion(EQUALS, &quot;1.5&quot;, usersV1Dot5);
 * </pre>
 *
 * <p>A request handler receiving a routed request may access the associated route's URI template variables via
 * {@link RouterContext#getUriTemplateVariables()}. For example, a request handler processing requests for the route
 * users/{userId} may obtain the value of {@code userId} as follows:</p>
 *
 * <pre>
 * String userId = context.asContext(RouterContext.class).getUriTemplateVariables().get(&quot;userId&quot;);
 * </pre>
 *
 * <p>The request handler receiving the routed request may access the associated resource's version via
 * {@link AcceptAPIVersionContext#getResourceVersion()}. For example, a request handler processing requests for a
 * version of a resource may obtain the resource's version as follows:</p>
 *
 * <pre>
 * Version resourceVersion = context.asContext(AcceptAPIVersionContext.class).getResourceVersion();
 * </pre>
 *
 * <p>During routing resource names are "relativized" by removing the leading path components which matched the
 * template. See the documentation for {@link RouterContext} for more information.</p>
 *
 * <p><b>NOTE:</b> for simplicity this implementation only supports a small sub-set of the functionality described in
 * RFC 6570.</p>
 *
 * @see RouterContext
 * @see AcceptAPIVersionContext
 * @see <a href="http://tools.ietf.org/html/rfc6570">RFC 6570 - URI Template</a>
 * @since 2.4.0
 */
public final class VersionRouter implements RequestHandler {

    private final Router uriRouter = new Router();
    private VersionSelector.DefaultVersionBehaviour defaultVersioningBehaviour =
            VersionSelector.DefaultVersionBehaviour.LATEST;
    private final Set<VersionRouterImpl> versionRouters = new CopyOnWriteArraySet<VersionRouterImpl>();

    /**
     * Creates a new router with no routes defined.
     */
    public VersionRouter() {
        // Nothing to do.
    }

    /**
     * Creates a new router containing the same routes and default route as the provided router. Changes to the
     * returned router's routing table will not impact the provided router.
     *
     * @param router The router to be copied.
     */
    public VersionRouter(VersionRouter router) {
        uriRouter.addAllRoutes(router.uriRouter);
    }

    /**
     * Adds all of the routes defined in the provided router to this router. New routes may be added while this router
     * is processing requests.
     *
     * @param router The router whose routes are to be copied into this router.
     * @return This router.
     */
    public VersionRouter addAllRoutes(VersionRouter router) {
        uriRouter.addAllRoutes(router.uriRouter);
        return this;
    }

    /**
     * <p>Adds a new route to this router for the provided collection resource provider. New routes may be added while
     * this router is processing requests.</p>
     *
     * <p>The provided URI template must match the resource collection itself, not resource instances. For example:</p>
     *
     * <pre>
     * CollectionResourceProvider users = ...;
     * Router router = new Router();
     *
     * // This is valid usage: the template matches the resource collection.
     * router.addRoute("users", users);
     *
     * // This is invalid usage: the template matches resource instances.
     * router.addRoute("users/{userId}", users);
     * </pre>
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @param provider The collection resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(String uriTemplate, CollectionResourceProvider provider) {
        return uriRouter.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a new route to this router for the provided singleton resource provider. New routes may be added while this
     * router is processing requests.
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @param provider The singleton resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(String uriTemplate, SingletonResourceProvider provider) {
        return uriRouter.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a new route to this router for the provided request handler. New routes may be added while this router is
     * processing requests.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The request handler to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        return uriRouter.addRoute(mode, uriTemplate, handler);
    }

    /**
     * <p>Initiates the creation of a new route, to a versioned resource, on this router for the provided URI template.
     * The route is not actually added to the router until a specific version and request handler has been specified by
     * calling #addVersion on the return {@code VersionHandler}.</p>
     *
     * <p>Use this method when adding routes to {@link CollectionResourceProvider}s and
     * {@link SingletonResourceProvider}s. To add routes to {@link RequestHandler}s use the
     * {@link #addRoute(RoutingMode, String)} method.</p>
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @return An {@code VersionHandler} instance to add resource version routes on.
     */
    public VersionHandler addRoute(String uriTemplate) {
        return addRoute(null, uriTemplate);
    }

    /**
     * <p>Initiates the creation of a new route, to a versioned resource, on this router for the provided URI template.
     * The route is not actually added to the router until a specific version and request handler has been specified by
     * calling #addVersion on the return {@code VersionHandler}.</p>
     *
     * <p>Use this method when adding routes to {@link RequestHandler}s. To add routes to
     * {@link CollectionResourceProvider}s and {@link SingletonResourceProvider}s use the
     * {@link #addRoute(String)} method.</p>
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @return An {@code VersionHandler} instance to add resource version routes on.
     */
    public VersionHandler addRoute(RoutingMode mode, String uriTemplate) {
        VersionRouterImpl versionRouter = new VersionRouterImpl();
        setVersionRouterDefaultBehaviour(versionRouter);
        return new VersionHandler(this, mode, versionRouter, uriTemplate);
    }

    /**
     * Sets the request handler to be used as the default route for requests which do not match any of the other
     * defined routes.
     *
     * @param handler The request handler to be used as the default route.
     * @return This router.
     */
    public VersionRouter setDefaultRoute(RequestHandler handler) {
        this.uriRouter.setDefaultRoute(handler);
        return this;
    }

    /**
     * Returns the request handler to be used as the default route for requests which do not match any of the other
     * defined routes.
     *
     * @return The request handler to be used as the default route.
     */
    public RequestHandler getDefaultRoute() {
        return uriRouter.getDefaultRoute();
    }

    private void setVersionRouterDefaultBehaviour(VersionRouterImpl versionRouter) {
        switch (defaultVersioningBehaviour) {
            case LATEST: {
                versionRouter.defaultToLatest();
                break;
            }
            case OLDEST: {
                versionRouter.defaultToOldest();
                break;
            }
            default: {
                versionRouter.noDefault();
            }
        }
    }

    private synchronized void updateVersionRoutersDefaultBehaviour() {
        for (VersionRouterImpl versionRouter : versionRouters) {
            setVersionRouterDefaultBehaviour(versionRouter);
        }
    }

    /**
     * Sets the behaviour of the version routing process to always use the latest resource version when the requested
     * version is {@code null}.
     */
    public VersionRouter setVersioningToDefaultToLatest() {
        defaultVersioningBehaviour = VersionSelector.DefaultVersionBehaviour.LATEST;
        updateVersionRoutersDefaultBehaviour();
        return this;
    }

    /**
     * Sets the behaviour of the version routing process to always use the oldest resource version when the requested
     * version is {@code null}.
     */
    public VersionRouter setVersioningToDefaultToOldest() {
        defaultVersioningBehaviour = VersionSelector.DefaultVersionBehaviour.OLDEST;
        updateVersionRoutersDefaultBehaviour();
        return this;
    }

    /**
     * Removes the default behaviour of the version routing process which will result in {@code NotFoundException}s when
     * the requested version is {@code null}.
     */
    public VersionRouter setVersioningBehaviourToNone() {
        defaultVersioningBehaviour = VersionSelector.DefaultVersionBehaviour.NONE;
        updateVersionRoutersDefaultBehaviour();
        return this;
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        uriRouter.handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        uriRouter.handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        uriRouter.handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        uriRouter.handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        uriRouter.handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        uriRouter.handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        uriRouter.handleUpdate(context, request, handler);
    }

    /**
     * Removes all of the routes from this router. Routes may be removed while this router is processing requests.
     *
     * @return This router.
     */
    public VersionRouter removeAllRoutes() {
        uriRouter.removeAllRoutes();
        return this;
    }

    /**
     * Removes one or more routes from this router. Routes may be removed while this router is processing requests.
     *
     * @param routes The routes to be removed.
     * @return {@code true} if at least one of the routes was found and removed.
     */
    public boolean removeRoute(final UriRoute... routes) {
        return uriRouter.removeRoute(routes);
    }

    /**
     * <p>A request handler which routes requests using a request resource version.</p>
     *
     * <p>Routes may be added to the router as follows:</p>
     *
     * <pre>
     * RequestHandler usersV1Dot0 = ...;
     * RequestHandler usersV1Dot5 = ...;
     * VersionRouter router = new VersionRouter();
     *
     * router.addVersion(EQUALS, &quot;1.0&quot;, usersV1Dot0)
     * .addVersion(EQUALS, &quot;1.5&quot;, usersV1Dot5);
     * </pre>
     *
     * <p>The request handler receiving the routed request may access the associated resource's version via
     * {@link AcceptAPIVersionContext#getResourceVersion()}. For example, a request handler processing requests for a
     * version of a resource may obtain the resource's version as follows:</p>
     *
     * <pre>
     * Version resourceVersion = context.asContext(AcceptAPIVersionContext.class).getResourceVersion();
     * </pre>
     *
     * @see AcceptAPIVersionContext
     * @since 2.4.0
     */
    static final class VersionRouterImpl implements RequestHandler {

        private static final VersionSelector VERSION_SELECTOR = new VersionSelector();

        private final Set<VersionRoute> routes = new CopyOnWriteArraySet<VersionRoute>();

        /**
         * Creates a new router with no routes defined.
         */
        VersionRouterImpl() {
            // Nothing to do
        }

        /**
         * Adds a new route to this router for the provided collection resource provider. New routes may be added while this
         * router is processing requests.
         *
         * @param version The version of the resource route which must match the requested version of the resource.
         * @param provider The collection resource provider to which matching requests will be routed.
         * @return An opaque handle for the route which may be used for removing the route later.
         */
        VersionRoute addVersion(String version, CollectionResourceProvider provider) {
            return addVersion(version, newCollection(provider));
        }

        /**
         * Adds a new route to this router for the provided singleton resource provider. New routes may be added while this
         * router is processing requests.
         *
         * @param version The version of the resource route which must match the requested version of the resource.
         * @param provider The singleton resource provider to which matching requests will be routed.
         * @return An opaque handle for the route which may be used for removing the route later.
         */
        VersionRoute addVersion(String version, SingletonResourceProvider provider) {
            return addVersion(version, newSingleton(provider));
        }

        /**
         * Adds a new route to this router for the provided request handler. New routes may be added while this router is
         * processing requests.
         *
         * @param version The version of the resource route which must match the requested version of the resource.
         * @param handler The request handler to which matching requests will be routed.
         * @return An opaque handle for the route which may be used for removing the route later.
         */
        VersionRoute addVersion(String version, RequestHandler handler) {
            return addVersion(new VersionRoute(Version.valueOf(version), handler));
        }

        private VersionRoute addVersion(VersionRoute route) {
            routes.add(route);
            return route;
        }

        /**
         * Sets the behaviour of the selection process to always use the latest resource version when the requested
         * version is {@code null}.
         */
        void defaultToLatest() {
            VERSION_SELECTOR.defaultToLatest();
        }

        /**
         * Sets the behaviour of the selection process to always use the oldest resource version when the requested
         * version is {@code null}.
         */
        void defaultToOldest() {
            VERSION_SELECTOR.defaultToOldest();
        }
        /**
         * Removes the default behaviour of the selection process which will result in {@code NotFoundException}s when
         * the requested version is {@code null}.
         */

        void noDefault() {
            VERSION_SELECTOR.noDefault();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            try {
                getBestRoute(context, request).handleAction(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
            try {
                getBestRoute(context, request).handleCreate(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
            try {
                getBestRoute(context, request).handleDelete(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
            try {
                getBestRoute(context, request).handlePatch(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
            try {
                getBestRoute(context, request).handleQuery(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
            try {
                getBestRoute(context, request).handleRead(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
            try {
                getBestRoute(context, request).handleUpdate(context, request, handler);
            } catch (NotFoundException e) {
                handler.handleError(e);
            }
        }

        private RequestHandler getBestRoute(ServerContext context, Request request) throws NotFoundException {
            AcceptAPIVersionContext apiVersionContext = context.asContext(AcceptAPIVersionContext.class);
            try {
                return VERSION_SELECTOR.select(apiVersionContext.getResourceVersion(), getRoutesMap());
            } catch (ResourceException e) {
                // TODO: i18n
                throw new NotFoundException(String.format("Version '%s' of resource '%s' not found",
                        apiVersionContext.getResourceVersion(), request.getResourceName()), e);
            }
        }

        private Map<Version, RequestHandler> getRoutesMap() {
            Map<Version , RequestHandler> routesMap = new HashMap<Version, RequestHandler>();
            for (VersionRoute route : routes) {
                routesMap.put(route.getVersion(), route.getRequestHandler());
            }
            return routesMap;
        }
    }
}
