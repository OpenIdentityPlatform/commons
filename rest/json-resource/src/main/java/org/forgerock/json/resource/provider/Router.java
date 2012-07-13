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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource.provider;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A resource provider which is capable of routing requests to a set of one or
 * more registered resource containers. Resource containers are associated with
 * a route which is specified as a URI template. Examples of valid URI templates
 * include:
 *
 * <pre>
 * /users
 * /users/{userId}/devices
 * </pre>
 *
 * Routers are thread safe and, in particular, support registration and
 * deregistration of resource containers while the router is handling requests.
 */
public final class Router implements ResourceProvider {
    private static final class Route {
        private final Object provider;
        private final UriTemplate template;

        Route(final UriTemplate template, final ResourceCollection provider) {
            this.template = template;
            this.provider = provider;
        }

        Route(final UriTemplate template, final ResourceSingleton provider) {
            this.template = template;
            this.provider = provider;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Route) {
                return template.equals(((Route) obj).template);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return template.hashCode();
        }

        ResourceCollection getCollectionResourceProvider() {
            return (ResourceCollection) provider;
        }

        ResourceSingleton getSingletonResourceProvider() {
            return (ResourceSingleton) provider;
        }

        RouteType getType() {
            return (provider instanceof ResourceCollection) ? RouteType.COLLECTION
                    : RouteType.SINGLETON;
        }

    }

    private static enum RouteType {
        COLLECTION, SINGLETON;
    }

    private static final class UriTemplate {
        // FIXME: need to normalize the template string so that /users/{foo}
        // matches /users/{bar}.
        private final String templateString;

        UriTemplate(final String template) {
            this.templateString = template;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof UriTemplate) {
                return templateString.equals(((UriTemplate) obj).templateString);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return templateString.hashCode();
        }
    }

    // The registered set of routes.
    private final Set<Route> routes = new CopyOnWriteArraySet<Route>();

    /**
     * Creates a new empty router.
     */
    public Router() {
        // No implementation required.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(final Context context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().actionInstance(context, request, handler);
                } else {
                    route.getCollectionResourceProvider().actionCollection(context, request,
                            handler);
                }
                break;
            case SINGLETON:
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in action request for singleton resource %s",
                            request.getComponent());
                } else {
                    route.getSingletonResourceProvider().actionInstance(context, request, handler);
                }
                break;
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Context context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                // Resource ID is optional for create requests.
                route.getCollectionResourceProvider().createInstance(context, request, null);
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be created", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Context context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().deleteInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in delete request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be deleted", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Deregisters the resource container associated with the specified URI
     * template, if present.
     *
     * @param uriTemplate
     *            The URI template of the resource container to be removed.
     * @return This router.
     */
    public Router deregisterResourceContainer(final String uriTemplate) {
        routes.remove(new Route(new UriTemplate(uriTemplate), (ResourceCollection) null));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patch(final Context context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().patchInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in patch request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().patchInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in patch request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void query(final Context context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in query request for resource collection %s",
                            request.getComponent());
                } else {
                    route.getCollectionResourceProvider()
                            .queryCollection(context, request, handler);
                }
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be queried", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(final Context context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().readInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in read request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().readInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in read request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Registers a resource collection with this router, replacing any existing
     * registered resource containers having the same URI template.
     *
     * @param route
     *            The URI template associated with the resource collection.
     * @param container
     *            The resource collection container.
     * @return This router.
     */
    public Router registerResourceContainer(final String route, final ResourceCollection container) {
        if (container == null) {
            throw new NullPointerException();
        }
        routes.add(new Route(new UriTemplate(route), container));
        return this;
    }

    /**
     * Registers a singleton resource with this router, replacing any existing
     * registered resource containers having the same URI template.
     *
     * @param uriTemplate
     *            The URI template associated with the singleton resource.
     * @param container
     *            The singleton resource provider.
     * @return This router.
     */
    public Router registerResourceContainer(final String uriTemplate,
            final ResourceSingleton container) {
        if (container == null) {
            throw new NullPointerException();
        }
        routes.add(new Route(new UriTemplate(uriTemplate), container));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Context context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().updateInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in update request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().updateInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in update request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    private Route findMatchingRoute(final Context context, final Request request)
            throws ResourceException {
        // TODO: find best match route
        // TODO: throw exception if no match is found
        // TODO: update context with template variables
        return null;
    }

    private ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

}
