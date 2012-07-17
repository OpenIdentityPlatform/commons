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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A request handler which routes resource requests to resource providers using
 * a configurable routing strategy.
 */
public final class Router implements RequestHandler {
    private final RoutingStrategy strategy;

    /**
     * Creates a new router which will use the provided routing strategy.
     *
     * @param strategy
     *            The routing strategy.
     */
    public Router(final RoutingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(final Context context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    route.asCollection().actionInstance(context, request, handler);
                } else {
                    route.asCollection().actionCollection(context, request, handler);
                }
            } else {
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in action request for singleton resource %s",
                            request.getComponent());
                } else {
                    route.asSingleton().actionInstance(context, request, handler);
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
    public void create(final Context context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                // Resource ID is optional for create requests.
                route.asCollection().createInstance(context, request, null);
            } else {
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
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    route.asCollection().deleteInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in delete request for resource collection %s",
                            request.getComponent());
                }
            } else {
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be deleted", request
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
    public void patch(final Context context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    route.asCollection().patchInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in patch request for resource collection %s",
                            request.getComponent());
                }
            } else {
                if (request.getResourceId() == null) {
                    route.asSingleton().patchInstance(context, request, null);
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
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in query request for resource collection %s",
                            request.getComponent());
                } else {
                    route.asCollection().queryCollection(context, request, handler);
                }
            } else {
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
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    route.asCollection().readInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in read request for resource collection %s",
                            request.getComponent());
                }
            } else {
                if (request.getResourceId() == null) {
                    route.asSingleton().readInstance(context, request, null);
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Router(");
        builder.append(strategy);
        builder.append(")");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Context context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final RoutingResult route = strategy.routeRequest(context, request);
            if (route.isCollection()) {
                if (request.getResourceId() != null) {
                    route.asCollection().updateInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in update request for resource collection %s",
                            request.getComponent());
                }
            } else {
                if (request.getResourceId() == null) {
                    route.asSingleton().updateInstance(context, request, null);
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

    private ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

}
