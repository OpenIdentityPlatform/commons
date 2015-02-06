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

import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.json.resource.VersionConstants.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;

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

    private static final String agentName = "CREST";
    private static final String EQUALS = "=";
    private static final String COMMA = ",";

    private final VersionSelector versionSelector = new VersionSelector();
    private final Map<Version, VersionRoute<RequestHandler>> routes =
            new ConcurrentHashMap<Version, VersionRoute<RequestHandler>>();
    private boolean warningEnabled = true;

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
        this.routes.putAll(router.routes);
    }
    /**
     * Adds all of the routes defined in the provided router to this router. New
     * routes may be added while this router is processing requests.
     *
     * @param router
     *            The router whose routes are to be copied into this router.
     * @return This router.
     */
    public VersionRouter addAllRoutes(VersionRouter router) {
        if (this != router) {
            routes.putAll(router.routes);
        }
        return this;
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
        addVersion(version, newCollection(provider));
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
        addVersion(version, newSingleton(provider));
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
        addVersion(new VersionRoute<RequestHandler>(Version.valueOf(version), handler));
        return this;
    }

    private void addVersion(VersionRoute<RequestHandler> route) {
        routes.put(route.getVersion(), route);
    }

    /**
     * Sets the behaviour of the version routing process to always use the latest resource version when the requested
     * version is {@code null}.
     */
    public VersionRouter defaultToLatest() {
        versionSelector.defaultToLatest();
        return this;
    }

    /**
     * Sets the behaviour of the version routing process to always use the oldest resource version when the requested
     * version is {@code null}.
     */
    public VersionRouter defaultToOldest() {
        versionSelector.defaultToOldest();
        return this;
    }

    /**
     * Removes the default behaviour of the version routing process which will result in {@code NotFoundException}s when
     * the requested version is {@code null}.
     */
    public VersionRouter noDefault() {
        versionSelector.noDefault();
        return this;
    }

    public VersionRouter setWarningEnabledBehaviour(boolean warningEnabled) {
        this.warningEnabled = warningEnabled;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            getBestRoute(context).handleAction(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            getBestRoute(context).handleCreate(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            getBestRoute(context).handleDelete(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        try {
            getBestRoute(context).handlePatch(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            getBestRoute(context).handleQuery(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            getBestRoute(context).handleRead(context, request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            getBestRoute(context).handleUpdate(context, request, handler);
        } catch (ResourceException e) {
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
     * @return The best matching {@code RequestHandler}
     * @throws NotFoundException If no match is found.
     */
    private RequestHandler getBestRoute(ServerContext context) throws ResourceException {
        AcceptAPIVersionContext apiVersionContext = context.asContext(AcceptAPIVersionContext.class);
        addWarningAdvice(context, apiVersionContext.getResourceVersion());
        final VersionRoute<RequestHandler> selectedRoute =
                versionSelector.select(apiVersionContext.getResourceVersion(), routes);
        addVersionAdvice(context, apiVersionContext.getProtocolVersion(), selectedRoute.getVersion());
        return selectedRoute.getRequestHandler();
    }

    private void addWarningAdvice(ServerContext context, Version version) {
        if (warningEnabled && version == null && context.containsContext(AdviceContext.class)) {
            AdviceContext adviceContext = context.asContext(AdviceContext.class);
            adviceContext.putAdvice("Warning", getVersionMissingAdvice(agentName, ACCEPT_API_VERSION).toString());
        }
    }

    static AdviceWarning getVersionMissingAdvice(String agentName, String headerName) {
        return AdviceWarning.getNotPresent(agentName, headerName);
    }

    private void addVersionAdvice(ServerContext context, Version protocolVersion, Version resourceVersion) {
        if (context.containsContext(AdviceContext.class)) {
            final AdviceContext adviceContext = context.asContext(AdviceContext.class);
            adviceContext.putAdvice(CONTENT_API_VERSION, new StringBuilder()
                    .append(PROTOCOL)
                    .append(EQUALS)
                    .append(protocolVersion.toString())
                    .append(COMMA)
                    .append(RESOURCE)
                    .append(EQUALS)
                    .append(resourceVersion.toString())
                    .toString());
        }
    }
}
