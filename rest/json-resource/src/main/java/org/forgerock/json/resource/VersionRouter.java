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
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;

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
public final class VersionRouter implements RequestHandler {

    private static final VersionSelector VERSION_SELECTOR = new VersionSelector();

    private final Set<VersionRoute> routes = new CopyOnWriteArraySet<VersionRoute>();
    private final Router router;
    private final RoutingMode mode;
    private final String uriTemplate;
    private RequestHandlerType requestHandlerType;
    private volatile Route uriRoute;

    /**
     * Creates a new router with no routes defined.
     */
    VersionRouter(Router router, RoutingMode mode, String uriTemplate) {
        Reject.ifNull(router, "router cannot be null.");
        Reject.ifNull(uriTemplate, "uriTemplate cannot be null.");
        this.router = router;
        this.mode = mode;
        this.uriTemplate = uriTemplate;
    }

    /**
     * Adds a new route to this router for the provided collection resource provider. New routes may be added while this
     * router is processing requests.
     *
     * @param version The version of the resource route which must match the requested version of the resource.
     * @param provider The collection resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public VersionRouter addVersion(String version, CollectionResourceProvider provider) {
        addVersion(RequestHandlerType.COLLECTION, STARTS_WITH, version, newCollection(provider));
        return this;
    }

    /**
     * Adds a new route to this router for the provided singleton resource provider. New routes may be added while this
     * router is processing requests.
     *
     * @param version The version of the resource route which must match the requested version of the resource.
     * @param provider The singleton resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public VersionRouter addVersion(String version, SingletonResourceProvider provider) {
        addVersion(RequestHandlerType.SINGLETON, EQUALS, version, newSingleton(provider));
        return this;
    }

    /**
     * Adds a new route to this router for the provided request handler. New routes may be added while this router is
     * processing requests.
     *
     * @param version The version of the resource route which must match the requested version of the resource.
     * @param handler The request handler to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public VersionRouter addVersion(String version, RequestHandler handler) {
        Reject.ifNull(mode, "Routing mode is not set. Incorrect use of Router#addRoute(String), "
                + "use Router#addRoute(RoutingMode, String) instead and specify the RoutingMode to use with the "
                + "RequestHandler.");
        return addVersion(RequestHandlerType.GENERIC, mode, version, handler);
    }

    private VersionRouter addVersion(RequestHandlerType type, RoutingMode mode, String version,
            RequestHandler handler) {
        addVersion(new VersionRoute(Version.valueOf(version), handler));
        addRoute(type, mode);
        return this;
    }

    private void addVersion(VersionRoute route) {
        routes.add(route);
    }

    /**
     * <P>If the route to the resource URI has not been added, this method will add it. This is only ever done once for
     * the very first call.</p>
     *
     * <p>Subsequent calls will be validated to ensure that the resource handler has not changed type, i.e. from a
     * CollectionResourceProvider to a SingletonResourceProvider.</p>
     *
     * @param type The resource handler type.
     * @param mode Indicates how the URI template should be matched against resource names.
     */
    private void addRoute(RequestHandlerType type, RoutingMode mode) {
        validateRequestHandlerType(type);
        if (uriRoute == null) {
            uriRoute = router.addRoute(mode, uriTemplate, this);
        }
    }

    private void validateRequestHandlerType(RequestHandlerType type) {
        if (requestHandlerType != null) {
            Reject.ifFalse(type.equals(requestHandlerType), "Incompatible request handler types, " + requestHandlerType
                    + " and " + type);
        } else {
            requestHandlerType = type;
        }
    }

    /**
     * Sets the behaviour of the version routing process to always use the latest resource version when the requested
     * version is {@code null}.
     */
    VersionRouter defaultToLatest() {
        VERSION_SELECTOR.defaultToLatest();
        return this;
    }

    /**
     * Sets the behaviour of the version routing process to always use the oldest resource version when the requested
     * version is {@code null}.
     */
    VersionRouter defaultToOldest() {
        VERSION_SELECTOR.defaultToLatest();
        return this;
    }

    /**
     * Removes the default behaviour of the version routing process which will result in {@code NotFoundException}s when
     * the requested version is {@code null}.
     */
    VersionRouter noDefault() {
        VERSION_SELECTOR.noDefault();
        return this;
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

    /**
     * <p>Selects the best match from the routes for the requested version.</p>
     *
     * <p>See {@link Version#isCompatibleWith(Version)} for information on the matching logic.</p>
     *
     * <p>If the requested version is {@code null} the the default behaviour is to return the latest route. This can be
     * changed by calling either {@link #defaultToLatest()} or {@link #defaultToOldest()}.</p>
     *
     * @param context The context.
     * @param request The request being processed.
     * @return The best matching {@code RequestHandler}
     * @throws NotFoundException If no match is found.
     */
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

    /**
     * An enum for the type of request handler for the resource.
     */
    private static enum RequestHandlerType {
        COLLECTION,
        SINGLETON,
        GENERIC
    }
}
